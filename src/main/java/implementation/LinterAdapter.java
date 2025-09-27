package implementation;

import com.google.gson.Gson;
import factory.LexerFactory;
import factory.LexerFactoryV1;
import factory.LexerFactoryV11;
import factory.ParserFactory;
import factory.ParserFactoryV1;
import factory.ParserFactoryV11;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import main.kotlin.analyzer.AnalysisResult;
import main.kotlin.analyzer.Analyzer;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.DefaultAnalyzer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import org.example.ast.ASTNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LinterAdapter implements PrintScriptLinter {
  private final Loader loader = new Loader();
  private final Gson gson = new Gson();

  @Override
  public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
    try {
      String code = readAll(src);

      LexerFactory lexerFactory = selectLexerFactory(version);
      ParserFactory parserFactory = selectParserFactory(version);
      Lexer lexer = lexerFactory.create();
      var parser = parserFactory.create();

      List<Token> tokens = lexer.tokenize(code);
      List<ASTNode> ast = parser.parse(tokens);

      Reader cfgReader = loader.streamToReader(config);
      LinterConfigAdapter cfgAdapter = gson.fromJson(cfgReader, LinterConfigAdapter.class);
      AnalyzerConfig analyzerCfg = cfgAdapter.toAnalyzerConfig();

      Analyzer analyzer = new DefaultAnalyzer();
      AnalysisResult result = analyzer.analyze(ast, analyzerCfg);

      // Report simple por el TCK (sin formatear demasiado)
      result.getDiagnostics().forEach(d ->
          handler.reportError(d.getMessage() + " at " + d.getPosition())
      );
    } catch (Throwable t) {
      handler.reportError("LinterAdapter failed: " + t.getMessage());
    }
  }

  private LexerFactory selectLexerFactory(String version) {
    if (version != null && version.trim().startsWith("1.1")) return new LexerFactoryV11();
    return new LexerFactoryV1();
  }

  private ParserFactory selectParserFactory(String version) {
    if (version != null && version.trim().startsWith("1.1")) return new ParserFactoryV11();
    return new ParserFactoryV1();
  }

  private String readAll(InputStream is) {
    try {
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String line;
      while ((line = br.readLine()) != null) sb.append(line).append('\n');
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
