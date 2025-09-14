package implementation;

import com.google.gson.Gson;
import main.kotlin.lexer.DefaultLexer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import main.kotlin.lexer.TokenProvider;
import org.example.LiteralNumber;
import org.example.LiteralString;
import org.example.TokenType;
import org.example.ast.ASTNode;
import org.example.formatter.FormatterVisitor;
import org.example.formatter.config.FormatterConfig;
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
import interpreter.PrintScriptFormatter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FormatterAdapter implements PrintScriptFormatter {
  private final Loader loader = new Loader();
  private final Gson gson = new Gson();

  @Override
  public void format(InputStream src, String version, InputStream config, Writer writer) {
    // 1) Build lexer with our preconfigured tokens
    Lexer lexer = new DefaultLexer(defaultTokenProvider());
    // 2) Lex
    String code = readAll(src);
    List<Token> tokens = lexer.tokenize(code);
    // 3) Parse to AST list
    List<ASTNode> ast = parseAll(tokens);
    // 4) Load/Adapt config
    FormatterConfigAdapter cfgAdapter = gson.fromJson(loader.streamToReader(config), FormatterConfigAdapter.class);
    FormatterConfig cfg = cfgAdapter.toConfig();
    // 5) Format each node
    StringBuilder out = new StringBuilder();
    for (ASTNode node : ast) {
      new FormatterVisitor(cfg, out).evaluate(node);
    }
    // 6) Write
    try { writer.write(out.toString()); } catch (IOException e) { throw new UncheckedIOException(e); }
  }

  private TokenProvider defaultTokenProvider() {
    Map<String, TokenType> map = new LinkedHashMap<>();

    // Palabras clave (deben ir antes que los identificadores)
    map.put("\\bnumber\\b", types.NumberType.INSTANCE);
    map.put("\\bstring\\b", types.StringType.INSTANCE);
    map.put("\\bconst\\b|\\blet\\b|\\bvar\\b", types.ModifierType.INSTANCE);

    // Asignación y operadores (primero los multi-char)
    map.put("==|!=|<=|>=", types.OperatorType.INSTANCE);
    map.put("=", types.AssignmentType.INSTANCE);
    map.put("[+\\-*/<>]", types.OperatorType.INSTANCE);

    // Puntuación necesaria para el lenguaje 1.0
    map.put(":", types.PunctuationType.INSTANCE);
    map.put(";", types.PunctuationType.INSTANCE);
    map.put("\\(", types.PunctuationType.INSTANCE);
    map.put("\\)", types.PunctuationType.INSTANCE);

    // Literales
    map.put("\"([^\"\\\\]|\\\\.)*\"", org.example.LiteralString.INSTANCE); // comillas dobles
    map.put("'([^'\\\\]|\\\\.)*'",   org.example.LiteralString.INSTANCE);  // comillas simples
    map.put("[0-9]+(?:\\.[0-9]+)?",  org.example.LiteralNumber.INSTANCE);

    // Identificadores (debe ir al final)
    map.put("[A-Za-z_][A-Za-z_0-9]*", types.IdentifierType.INSTANCE);

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
