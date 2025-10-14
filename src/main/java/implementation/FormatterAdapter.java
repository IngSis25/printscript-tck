package implementation;

import com.google.gson.Gson;
import interpreter.PrintScriptFormatter;
import factory.LexerFactory;
import factory.LexerFactoryV1;
import factory.LexerFactoryV11;
import factory.ParserFactory;
import factory.ParserFactoryV1;
import factory.ParserFactoryV11;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.Token;
import org.example.ast.ASTNode;
import org.example.formatter.FormatterVisitor;
import org.example.formatter.Formatter;
import org.example.formatter.config.FormatterConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FormatterAdapter implements PrintScriptFormatter {
    private final Loader loader = new Loader();
    private final Gson gson = new Gson();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        try {
            // 1) Source
            String code = readAll(src);

            // 2) Factories reales (sin reglas pegadas al adapter)
            LexerFactory lexerFactory = selectLexerFactory(version);
            ParserFactory parserFactory = selectParserFactory(version);
            Lexer lexer = lexerFactory.create();
            var parser = parserFactory.create();

            // 3) Lex + Parse
            List<Token> tokens = lexer.tokenize(code);
            List<ASTNode> ast = parser.parse(tokens);

            // 4) Config del formatter (adapter -> FormatterConfig)
            Reader cfgReader = loader.streamToReader(config);
            FormatterConfigAdapter cfgAdapter = gson.fromJson(cfgReader, FormatterConfigAdapter.class);
            FormatterConfig cfg = cfgAdapter.toConfig();

            // 5) Visita y escritura normal con todas las reglas
            StringBuilder out = new StringBuilder();
            new FormatterVisitor(cfg, out).evaluateMultiple(ast);
            writer.write(out.toString());
        } catch (Throwable t) {
            throw new RuntimeException("FormatterAdapter failed: " + t.getMessage(), t);
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

    // Lee el archivo preservando exactamente el formato original (sin agregar \n)
    private String readAllPreservingFormat(InputStream is) {
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}