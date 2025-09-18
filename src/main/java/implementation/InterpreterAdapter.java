package implementation;

import factory.LexerFactory;
import factory.LexerFactoryV1;
import factory.LexerFactoryV11;
import factory.ParserFactory;
import factory.ParserFactoryV1;
import factory.ParserFactoryV11;
import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import org.example.DefaultInterpreter;
import org.example.ast.ASTNode;
import org.example.ast.BlockNode;
import org.example.strategy.PreConfiguredProviders;
import org.example.strategy.StrategyProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class InterpreterAdapter implements PrintScriptInterpreter {

  @Override
  public void interpret(InputStream src, String version, InputProvider input, PrintEmitter output, ErrorHandler handler) {
    try {
      String code = readAll(src);

      LexerFactory lexerFactory = selectLexerFactory(version);
      ParserFactory parserFactory = selectParserFactory(version);
      Lexer lexer = lexerFactory.create();
      var parser = parserFactory.create();

      List<Token> tokens = lexer.tokenize(code);
      List<ASTNode> ast = parser.parse(tokens);

      StrategyProvider provider = selectProvider(version);
      OutputAdapter out = new OutputAdapter(output);
      DefaultInterpreter interpreter = new DefaultInterpreter(out, provider);

      // 🚫 NO crear BlockNode artificial
      // ✅ Si el parser devolvió un BlockNode raíz, lo desempaquetamos
      if (ast.size() == 1 && ast.get(0) instanceof BlockNode) {
        BlockNode block = (BlockNode) ast.get(0);
        for (ASTNode stmt : block.getStatements()) {
          interpreter.interpret(stmt);
        }
      } else {
        for (ASTNode node : ast) {
          interpreter.interpret(node);
        }
      }
    } catch (Throwable t) {
      handler.reportError("InterpreterAdapter failed: " + t.getMessage());
    }
  }

  @Override
  public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
    interpret(src, version, provider, emitter, handler);
  }

  private LexerFactory selectLexerFactory(String version) {
    if (version != null && version.trim().startsWith("1.1")) return new LexerFactoryV11();
    return new LexerFactoryV1();
  }

  private ParserFactory selectParserFactory(String version) {
    if (version != null && version.trim().startsWith("1.1")) return new ParserFactoryV11();
    return new ParserFactoryV1();
  }

  private StrategyProvider selectProvider(String version) {
    if (version != null && version.trim().startsWith("1.1")) {
      return PreConfiguredProviders.INSTANCE.getVERSION_1_1(); // 👈 Kotlin object desde Java
    }
    return PreConfiguredProviders.INSTANCE.getVERSION_1_0();
  }

  private String readAll(InputStream is) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) sb.append(line).append('\n');
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
