package implementation.formatter;

import com.google.gson.Gson;
import interpreter.PrintScriptFormatter;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.TokenFactory;
import org.Parser;
import org.ParserFactory;
import org.example.astnode.ASTNode;
import org.example.formatter.Formatter;
import org.example.formatter.config.FormatterConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MyPrintScriptFormatter implements PrintScriptFormatter {

    private final Gson gson = new Gson();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        try {
            // Leer código y config
            String code = readStream(src);
            FormatterConfig cfg = gson.fromJson(new InputStreamReader(config, StandardCharsets.UTF_8), FormatterConfig.class);

            // Crear Lexer y Parser usando TUS factories
            TokenFactory tokenFactory = new TokenFactory();
            var tokenResolver = version.startsWith("1.1") ? tokenFactory.createLexerV11() : tokenFactory.createLexerV10();
            Lexer lexer = new Lexer(tokenResolver, new StringReader(code));
            Parser parser = version.startsWith("1.1") ? ParserFactory.INSTANCE.createParserV11(lexer) : ParserFactory.INSTANCE.createParserV10(lexer);
            
            // Obtener AST nodes
            List<ASTNode> ast = new ArrayList<>();
            while (parser.hasNext()) ast.add(parser.next());

            // Formatear y escribir (tu Formatter se encarga de todo)
            String result = new Formatter(cfg).format(ast.iterator());
            writer.write(result.endsWith("\n") ? result.substring(0, result.length() - 1) : result);
        } catch (Exception e) {
            throw new RuntimeException("Formatter error: " + e.getMessage(), e);
        }
    }

    private String readStream(InputStream stream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

