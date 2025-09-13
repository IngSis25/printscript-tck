package implementation;

import com.google.gson.Gson;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import main.kotlin.analyzer.Analyzer;
import main.kotlin.analyzer.AnalysisResult;
import main.kotlin.analyzer.DefaultAnalyzer;
import main.kotlin.lexer.DefaultLexer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import main.kotlin.lexer.TokenProvider;
import org.example.TokenType;
import org.example.ast.ASTNode;
import parser.rules.ParserRule;
import parser.rules.VariableDeclarationRule;
import rules.ExpressionRule;
import rules.PrintlnRule;
import rules.MatchedRule;
import rules.RuleMatcher;
import builders.ExpressionBuilder;
import builders.PrintBuilder;
import builders.VariableDeclarationBuilder;
import main.kotlin.parser.ParseResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LinterAdapter implements PrintScriptLinter {
  private final Loader loader = new Loader();
  private final Gson gson = new Gson();

  @Override
  public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
    try {
      // 1) lex+parse
      Lexer lexer = new DefaultLexer(defaultTokenProvider());
      String code = readAll(src);
      List<Token> tokens = lexer.tokenize(code);
      List<ASTNode> ast = parseAll(tokens);
      // 2) config
      LinterConfigAdapter cfgAdapter = gson.fromJson(loader.streamToReader(config), LinterConfigAdapter.class);
      main.kotlin.analyzer.AnalyzerConfig cfg = cfgAdapter.toConfig();
      // 3) analyze
      Analyzer analyzer = new DefaultAnalyzer();
      AnalysisResult result = analyzer.analyze(ast, cfg);
      result.getDiagnostics().forEach(d -> handler.reportError(d.getMessage()));
    } catch (Throwable t) {
      handler.reportError(t.getMessage() == null ? t.toString() : t.getMessage());
    }
  }

  private TokenProvider defaultTokenProvider() {
    Map<String, TokenType> map = new LinkedHashMap<>();
    map.put("\\bnumber\\b", types.NumberType.INSTANCE);
    map.put("\\bstring\\b", types.StringType.INSTANCE);
    map.put("\\bconst\\b|\\blet\\b|\\bvar\\b", types.ModifierType.INSTANCE);
    map.put("=", types.AssignmentType.INSTANCE);
    map.put("==|!=|<=|>=", types.OperatorType.INSTANCE);
    map.put("[+\\-*/<>]", types.OperatorType.INSTANCE);
    map.put("\"([^\"\\\\]|\\\\.)*\"", org.example.LiteralString.INSTANCE);
    map.put("[0-9]+(?:\\.[0-9]+)?", org.example.LiteralNumber.INSTANCE);
    map.put("[A-Za-z_][A-Za-z_0-9]*", main.kotlin.lexer.IdentifierType.INSTANCE);
    map.put(";", types.PunctuationType.INSTANCE);
    map.put("\\(", types.PunctuationType.INSTANCE);
    map.put("\\)", types.PunctuationType.INSTANCE);
    return TokenProvider.Companion.fromMap(map);
  }

  private List<ASTNode> parseAll(List<Token> tokens) {
    List<ParserRule> rules = Arrays.asList(
        new PrintlnRule(new PrintBuilder()),
        new VariableDeclarationRule(new VariableDeclarationBuilder()),
        new ExpressionRule(new ExpressionBuilder())
    );
    RuleMatcher matcher = new RuleMatcher(rules);
    List<ASTNode> ast = new ArrayList<>();
    int pos = 0;
    while (pos < tokens.size()) {
      ParseResult<MatchedRule> res = matcher.matchNext(tokens, pos);
      if (res instanceof ParseResult.Success) {
        ParseResult.Success<MatchedRule> s = (ParseResult.Success<MatchedRule>) res;
        MatchedRule matched = s.getNode();
        ASTNode node = matched.getRule().getBuilder().buildNode(matched.getMatchedTokens());
        ast.add(node);
        pos = s.getNextPosition();
      } else if (res instanceof ParseResult.Failure) {
        ParseResult.Failure f = (ParseResult.Failure) res;
        throw new IllegalArgumentException("Syntax error at " + f.getPosition() + ": " + f.getMessage());
      } else {
        throw new IllegalArgumentException("No rule matched at position " + pos);
      }
    }
    return ast;
  }

  private static String readAll(InputStream in) {
    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) { sb.append(line).append('\n'); }
      return sb.toString();
    } catch (IOException e) { throw new UncheckedIOException(e); }
  }
}
