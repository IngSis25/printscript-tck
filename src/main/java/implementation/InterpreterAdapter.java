package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import main.kotlin.lexer.DefaultLexer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import main.kotlin.lexer.TokenProvider;
import main.kotlin.parser.ParseResult;
import org.example.TokenType;
import org.example.ast.ASTNode;
import org.example.output.Output;
import org.example.strategy.PreConfiguredProviders;
import org.example.strategy.Strategy;
import org.example.strategy.StrategyProvider;
import org.example.util.Services;
import parser.rules.ParserRule;
import parser.rules.VariableDeclarationRule;
import rules.ExpressionRule;
import rules.MatchedRule;
import rules.PrintlnRule;
import rules.RuleMatcher;
import builders.ExpressionBuilder;
import builders.PrintBuilder;
import builders.VariableDeclarationBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

public class InterpreterAdapter implements PrintScriptInterpreter {

  @Override
  public void interpret(InputStream src, String version, InputProvider input, PrintEmitter output, ErrorHandler handler) {
    try {
      // 1) lex + parse
      Lexer lexer = new DefaultLexer(defaultTokenProvider());
      String code = readAll(src);
      List<Token> tokens = lexer.tokenize(code);
      List<ASTNode> ast = parseAll(tokens);

      // 2) runtime primitives
      StrategyProvider strategies = PreConfiguredProviders.INSTANCE.getVERSION_1_0();
      Output out = new OutputAdapter(output);

      // visit function
      BiFunction<Services, ASTNode, Object> visit = new BiFunction<Services, ASTNode, Object>() {
        @Override public Object apply(Services services, ASTNode node) {
          @SuppressWarnings("unchecked")
          Strategy<ASTNode> strategy = (Strategy<ASTNode>) strategies.getStrategyFor(node);
          if (strategy == null) throw new IllegalStateException("No strategy for node " + node.getClass());
          return strategy.visit(services, node);
        }
      };

      Services base = new Services(Collections.emptyMap(), out, (s, n) -> visit.apply(s, n));

      // 3) run
      for (ASTNode node : ast) {
        visit.apply(base, node);
      }
    } catch (Throwable t) {
      String msg = t.getMessage();
      handler.reportError(msg == null ? t.toString() : msg.split(":")[0]);
    }
  }

  // --- Si en algún lado te llaman execute(...), lo delegamos a interpret(...) ---
  public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
    interpret(src, version, provider, emitter, handler);
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
