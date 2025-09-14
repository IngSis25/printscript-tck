package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import main.kotlin.lexer.DefaultLexer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import main.kotlin.lexer.TokenProvider;
import main.kotlin.lexer.TokenRule;
import types.*;
import types.PrintlnType;
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
import kotlin.text.Regex;

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

      // 3) run - Actualizar Services después de cada nodo
      Services currentServices = new Services(Collections.emptyMap(), out, (s, n) -> visit.apply(s, n));
      
      for (ASTNode node : ast) {
        Object result = visit.apply(currentServices, node);
        // Actualizar el Services con el resultado del nodo para mantener el estado
        // Esto asegura que las variables declaradas estén disponibles para los siguientes nodos
        if (result != null) {
          // Si el nodo devuelve un Services actualizado, usarlo
          if (result instanceof Services) {
            currentServices = (Services) result;
          }
        }
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
    // Reglas ignoradas (exactamente como en ConfiguredTokens.kt)
    List<TokenRule> ignoredRules = Arrays.asList(
        new TokenRule(new Regex("\\G[ \\t]+"), types.PunctuationType.INSTANCE, true),
        new TokenRule(new Regex("\\G(?:\\r?\\n)+"), types.PunctuationType.INSTANCE, true),
        new TokenRule(new Regex("\\G//.*(?:\\r?\\n|$)"), types.PunctuationType.INSTANCE, true)
    );
    
    // Reglas de tokens V1 (exactamente como en ConfiguredTokens.V1)
    Map<String, TokenType> v1Map = new LinkedHashMap<>();
    
    // Palabras clave (deben ir antes que los identificadores)
    v1Map.put("\\bnumber\\b", types.NumberType.INSTANCE);
    v1Map.put("\\bstring\\b", types.StringType.INSTANCE);
    v1Map.put("\\blet\\b|\\bvar\\b", types.ModifierType.INSTANCE);
    v1Map.put("\\bprintln\\b", types.PrintlnType.INSTANCE);
    
  
    v1Map.put("==|!=|<=|>=", types.OperatorType.INSTANCE);
    v1Map.put("=", types.AssignmentType.INSTANCE);
    v1Map.put("[+\\-*/<>]", types.OperatorType.INSTANCE);
    
    // Puntuación necesaria para el lenguaje 1.0
    v1Map.put(":", types.PunctuationType.INSTANCE);
    v1Map.put(";", types.PunctuationType.INSTANCE);
    v1Map.put("\\(", types.PunctuationType.INSTANCE);
    v1Map.put("\\)", types.PunctuationType.INSTANCE);
    

    v1Map.put("\"([^\"\\\\]|\\\\.)*\"", org.example.LiteralString.INSTANCE);
    v1Map.put("[0-9]+(?:\\.[0-9]+)?", org.example.LiteralNumber.INSTANCE);

    v1Map.put("[A-Za-z_][A-Za-z_0-9]*", types.IdentifierType.INSTANCE);
    
    // Combinar exactamente como ConfiguredTokens.providerV1()
    List<TokenRule> allRules = new ArrayList<>(ignoredRules);
    allRules.addAll(TokenProvider.Companion.fromMap(v1Map).rules());
    
    return new TokenProvider(allRules);
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
