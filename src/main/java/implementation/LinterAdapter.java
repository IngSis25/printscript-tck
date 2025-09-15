package implementation;

import com.google.gson.Gson;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import kotlin.text.Regex;
import main.kotlin.analyzer.Analyzer;
import main.kotlin.analyzer.AnalysisResult;
import main.kotlin.analyzer.DefaultAnalyzer;
import main.kotlin.lexer.*;
import org.example.LiteralNumber;
import org.example.LiteralString;
import org.example.TokenType;
import org.example.ast.ASTNode;
import parser.rules.ParserRule;

import rules.*;
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
    List<TokenRule> rules = new ArrayList<>();

    // --- IGNORADOS (si tu formatter no distingue newline de space, podés dejar solo \\G\\s+) ---
    rules.add(new TokenRule(new Regex("\\G[ \\t]+"), types.WhitespaceType.INSTANCE, true));          // espacios/tabs
    rules.add(new TokenRule(new Regex("\\G(?:\\r?\\n)+"), types.WhitespaceType.INSTANCE, true));     // newlines
    // rules.add(new TokenRule(new Regex("\\G/\\*[\\s\\S]*?\\*/"), types.CommentType.INSTANCE, true)); // comentarios bloque (opcional)

    // --- KEYWORDS (antes que Identifier) ---
    rules.add(new TokenRule(new Regex("\\G\\bprintln\\b"), types.PrintlnType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\bnumber\\b"), types.NumberType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\bstring\\b"), types.StringType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\b(?:const|let|var)\\b"), types.ModifierType.INSTANCE, false));

    // --- PUNTUACIÓN ---
    rules.add(new TokenRule(new Regex("\\G:"), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G;"), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\("), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\)"), types.PunctuationType.INSTANCE, false));

    // --- OPERADORES (multi-char antes que single) ---
    rules.add(new TokenRule(new Regex("\\G(?:==|!=|<=|>=)"), types.OperatorType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G="), types.AssignmentType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G[+\\-*/<>]"), types.OperatorType.INSTANCE, false));

    // --- LITERALES ---
    rules.add(new TokenRule(new Regex("\\G\"([^\"\\\\]|\\\\.)*\""), LiteralString.INSTANCE, false)); // dobles
    rules.add(new TokenRule(new Regex("\\G'([^'\\\\]|\\\\.)*'"),  LiteralString.INSTANCE, false));  // simples
    rules.add(new TokenRule(new Regex("\\G[0-9]+(?:\\.[0-9]+)?"), LiteralNumber.INSTANCE, false));

    // --- IDENTIFIER (después de keywords) ---
    rules.add(new TokenRule(new Regex("\\G[A-Za-z_][A-Za-z_0-9]*"), types.IdentifierType.INSTANCE, false));

    return TokenProvider.Companion.fromRules(rules);
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
