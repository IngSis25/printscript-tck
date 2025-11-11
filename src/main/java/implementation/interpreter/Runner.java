package implementation.interpreter;

import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.TokenFactory;
import org.Parser;
import org.ParserFactory;
import org.example.Interpreter;
import org.example.input.Input;
import org.example.output.Output;
import org.example.strategy.PreConfiguredProviders;

import java.io.BufferedReader;
import java.io.Reader;

public class Runner {
    private final String version;
    private final String sourceCode;

    public Runner(String version, Reader reader) {
        this.version = version;
        this.sourceCode = readAll(reader);
    }

    public void execute(String version, Output output, Input input) {
        // 1. Crear Lexer
        TokenFactory tokenFactory = new TokenFactory();
        var tokenResolver = version.startsWith("1.1") 
            ? tokenFactory.createLexerV11() 
            : tokenFactory.createLexerV10();
        Lexer lexer = new Lexer(tokenResolver, new java.io.StringReader(sourceCode));

        // 2. Crear Parser
        Parser parser = version.startsWith("1.1")
            ? ParserFactory.INSTANCE.createParserV11(lexer)
            : ParserFactory.INSTANCE.createParserV10(lexer);

        // 3. Crear Interpreter con output, input y strategyProvider
        var strategyProvider = version.startsWith("1.1")
            ? PreConfiguredProviders.INSTANCE.getVERSION_1_1()
            : PreConfiguredProviders.INSTANCE.getVERSION_1_0();
        
        Interpreter interpreter = new Interpreter(output, input, strategyProvider);

        // 4. Interpretar
        interpreter.interpret(parser);
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
}

