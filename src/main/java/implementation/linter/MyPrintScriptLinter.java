package implementation.linter;

import com.google.gson.Gson;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import main.kotlin.analyzer.AnalysisResult;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.DefaultAnalyzer;
import main.kotlin.lexer.Lexer;
import main.kotlin.lexer.TokenFactory;
import org.Parser;
import org.ParserFactory;
import org.example.astnode.ASTNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static implementation.util.InputStreamToStringReader.convert;

public class MyPrintScriptLinter implements PrintScriptLinter {
    
    private final Gson gson = new Gson();

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        try {
            // 1. Leer código fuente
            Reader reader = convert(src);
            String code = readAll(reader);

            // 2. Crear Lexer
            TokenFactory tokenFactory = new TokenFactory();
            var tokenResolver = version.startsWith("1.1") 
                ? tokenFactory.createLexerV11() 
                : tokenFactory.createLexerV10();
            Lexer lexer = new Lexer(tokenResolver, new StringReader(code));

            // 3. Crear Parser
            Parser parser = version.startsWith("1.1")
                ? ParserFactory.INSTANCE.createParserV11(lexer)
                : ParserFactory.INSTANCE.createParserV10(lexer);

            // 4. Obtener todos los AST nodes
            List<ASTNode> ast = new ArrayList<>();
            while (parser.hasNext()) {
                ast.add(parser.next());
            }

            // 5. Leer configuración del linter
            Reader cfgReader = new InputStreamReader(config, StandardCharsets.UTF_8);
            com.google.gson.JsonObject jsonConfig = gson.fromJson(cfgReader, com.google.gson.JsonObject.class);
            
            // Transformar JSON del TCK al formato que espera AnalyzerVisitorsFactory
            com.google.gson.JsonObject transformedJson = transformTckJsonToAnalyzerJson(jsonConfig, version);
            
            LinterConfigAdapter cfgAdapter = gson.fromJson(jsonConfig, LinterConfigAdapter.class);
            AnalyzerConfig analyzerCfg = cfgAdapter.toAnalyzerConfig(transformedJson);

            // 6. Crear analyzer y analizar
            DefaultAnalyzer analyzer = new DefaultAnalyzer(version, parser);
            AnalysisResult result = analyzer.analyze(ast, analyzerCfg, version);

            // 7. Reportar diagnósticos
            result.getDiagnostics().forEach(d ->
                handler.reportError(d.getMessage() + " at " + d.getPosition())
            );
        } catch (Exception e) {
            handler.reportError("MyPrintScriptLinter failed: " + e.getMessage());
        }
    }

    private String readAll(Reader reader) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(reader)) {
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

    /**
     * Transforma el JSON del TCK al formato que espera AnalyzerVisitorsFactory.
     * 
     * TCK format: {"identifier_format": "camel case", "mandatory_variable_or_literal_in_println": true}
     * Analyzer format: {"NamingFormatCheck": {...}, "UnusedVariableCheck": {...}, ...}
     */
    private com.google.gson.JsonObject transformTckJsonToAnalyzerJson(com.google.gson.JsonObject tckJson, String version) {
        com.google.gson.JsonObject result = new com.google.gson.JsonObject();
        
        // identifier_format -> NamingFormatCheck
        if (tckJson.has("identifier_format")) {
            String format = tckJson.get("identifier_format").getAsString().trim();
            String namingPattern = format.equalsIgnoreCase("camel case") ? "camelCase" : "snake_case";
            
            com.google.gson.JsonObject namingCheck = new com.google.gson.JsonObject();
            namingCheck.addProperty("namingPatternName", namingPattern);
            result.add("NamingFormatCheck", namingCheck);
        }
        
        // mandatory_variable_or_literal_in_println o mandatory-variable-or-literal-in-println -> PrintUseCheck
        if (tckJson.has("mandatory_variable_or_literal_in_println") || tckJson.has("mandatory-variable-or-literal-in-println")) {
            boolean enabled = tckJson.has("mandatory_variable_or_literal_in_println")
                ? tckJson.get("mandatory_variable_or_literal_in_println").getAsBoolean()
                : tckJson.get("mandatory-variable-or-literal-in-println").getAsBoolean();
            
            com.google.gson.JsonObject printCheck = new com.google.gson.JsonObject();
            printCheck.addProperty("printlnCheckEnabled", enabled);
            result.add("PrintUseCheck", printCheck);
        }
        
        // readInputCheckEnabled, read-input-check-enabled o mandatory-variable-or-literal-in-readInput -> ReadInputCheck (solo para v1.1)
        if (version.startsWith("1.1") && 
            (tckJson.has("read_input_check_enabled") || 
             tckJson.has("read-input-check-enabled") ||
             tckJson.has("mandatory-variable-or-literal-in-readInput") ||
             tckJson.has("mandatory_variable_or_literal_in_readInput"))) {
            
            boolean enabled = false;
            if (tckJson.has("read_input_check_enabled")) {
                enabled = tckJson.get("read_input_check_enabled").getAsBoolean();
            } else if (tckJson.has("read-input-check-enabled")) {
                enabled = tckJson.get("read-input-check-enabled").getAsBoolean();
            } else if (tckJson.has("mandatory-variable-or-literal-in-readInput")) {
                enabled = tckJson.get("mandatory-variable-or-literal-in-readInput").getAsBoolean();
            } else if (tckJson.has("mandatory_variable_or_literal_in_readInput")) {
                enabled = tckJson.get("mandatory_variable_or_literal_in_readInput").getAsBoolean();
            }
            
            com.google.gson.JsonObject readCheck = new com.google.gson.JsonObject();
            readCheck.addProperty("readInputCheckEnabled", enabled);
            result.add("ReadInputCheck", readCheck);
        }
        
        return result;
    }
}

