package implementation.formatter;

import main.kotlin.lexer.LexerFactory;
import org.Parser;
import org.ParserFactory;
import org.example.formatter.FormatResult;
import org.example.formatter.Formatter;
import org.example.formatter.RulesFactory;
import rules.Rule;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;

public class Runner {
    private final String version;
    private final String sourceCode;

    public Runner(String version, Reader reader) {
        this.version = version;
        this.sourceCode = readAll(reader);
    }

    public FormattedResult format(String configJson, String version) {
        try {
            // Crear Lexer según la versión
            var lexer = version.startsWith("1.1") 
                ? LexerFactory.INSTANCE.createLexerV11(new java.io.StringReader(sourceCode))
                : LexerFactory.INSTANCE.createLexerV10(new java.io.StringReader(sourceCode));
            
            // Crear Parser según la versión
            Parser parser = version.startsWith("1.1")
                ? ParserFactory.INSTANCE.createParserV11(lexer)
                : ParserFactory.INSTANCE.createParserV10(lexer);
            
            // Crear el Formatter (el parser implementa PrintScriptIterator) pasando también el source original
            Formatter formatter = new Formatter(parser, sourceCode);
            
            // Crear las reglas desde el JSON
            RulesFactory rulesFactory = new RulesFactory();
            List<Rule> rules = rulesFactory.getRules(configJson, version);
            
            // Formatear el código
            FormatResult result = formatter.format(rules);
            String formattedCode = result.getCode();
            
            // Eliminar todos los saltos de línea al final
            while (formattedCode.endsWith("\n")) {
                formattedCode = formattedCode.substring(0, formattedCode.length() - 1);
            }
            
            return new FormattedResult(formattedCode);
            
        } catch (Exception e) {
            throw new RuntimeException("Formatter error: " + e.getMessage(), e);
        }
    }

    private String readAll(Reader reader) {
        try (BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class FormattedResult {
        private final String formattedCode;

        public FormattedResult(String formattedCode) {
            this.formattedCode = formattedCode;
        }

        public String getFormattedCode() {
            return formattedCode;
        }
    }
}

