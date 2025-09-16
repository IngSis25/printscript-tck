package implementation;

import builders.*;
import com.google.gson.Gson;
import kotlin.text.Regex;
import main.kotlin.lexer.*;
import org.example.LiteralNumber;
import org.example.LiteralString;
import org.example.TokenType;
import org.example.ast.ASTNode;
import org.example.formatter.FormatterVisitor;
import org.example.formatter.config.FormatterConfig;
import parser.rules.AssignmentRule;
import parser.rules.ParserRule;

import rules.*;
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
        // 2) Lex
        String code = readAll(src);

        // 4) Load/Adapt config PRIMERO para detectar mandatory-line-break-after-statement
        FormatterConfigAdapter cfgAdapter = gson.fromJson(loader.streamToReader(config), FormatterConfigAdapter.class);

        if (cfgAdapter.mandatoryLineBreakAfterStatement != null && cfgAdapter.mandatoryLineBreakAfterStatement) {
            // Usar estrategia de preservar espacios originales
            String result = formatWithMandatoryLineBreaks(code);
            try {
                writer.write(result);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }

        // 1) Build lexer with our preconfigured tokens
        Lexer lexer = new DefaultLexer(defaultTokenProvider());
        List<Token> tokens = lexer.tokenize(code);
        // 3) Parse to AST list
        List<ASTNode> ast = parseAll(tokens);

        FormatterConfig cfg = cfgAdapter.toConfig();

        StringBuilder out = new StringBuilder();
        FormatterVisitor visitor = new FormatterVisitor(cfg, out);
        visitor.evaluateMultiple(ast);

        if (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
            out.deleteCharAt(out.length() - 1);
        }
        try { writer.write(out.toString()); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private String formatWithMandatoryLineBreaks(String code) {
        String[] statements = code.split(";");
        List<String> nonEmptyStatements = new ArrayList<>();

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                nonEmptyStatements.add(trimmed);
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < nonEmptyStatements.size(); i++) {
            result.append(nonEmptyStatements.get(i)).append(";");

            if (i < nonEmptyStatements.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private TokenProvider defaultTokenProvider() {
        List<TokenRule> rules = new ArrayList<>();

        // --- IGNORADOS (espacios y newlines) ---
        rules.add(new TokenRule(new Regex("\\G[ \\t]+"), types.WhitespaceType.INSTANCE, true));          // espacios/tabs
        rules.add(new TokenRule(new Regex("\\G(?:\\r?\\n)+"), types.WhitespaceType.INSTANCE, true));     // newlines

        // --- KEYWORDS (antes que Identifier) ---
        rules.add(new TokenRule(new Regex("\\G\\bprintln\\b"), types.PrintlnType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G\\bnumber\\b"), types.NumberType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G\\bstring\\b"), types.StringType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G\\b(?:const|let|var)\\b"), types.ModifierType.INSTANCE, false));

        // --- PUNTUACIÓN ---
        rules.add(new TokenRule(new Regex("\\G:"), types.PunctuationType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G;"), types.PunctuationType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G\\("), types.PunctuationType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G\\)"), types.PunctuationType.INSTANCE, false));

        // --- OPERADORES (multi-char antes que single) ---
        rules.add(new TokenRule(new Regex("\\G(?:==|!=|<=|>=)"), types.OperatorType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G="), types.AssignmentType.INSTANCE, false));
        rules.add(new TokenRule(new Regex("\\G[+\\-*/<>]"), types.OperatorType.INSTANCE, false));


        // --- LITERALES ---
        rules.add(new TokenRule(new Regex("\\G\"([^\"\\\\]|\\\\.)*\""), LiteralString.INSTANCE, false)); // dobles
        rules.add(new TokenRule(new Regex("\\G'([^'\\\\]|\\\\.)*'"),  LiteralString.INSTANCE, false));  // simples
        rules.add(new TokenRule(new Regex("\\G[0-9]+(?:\\.[0-9]+)?"), LiteralNumber.INSTANCE, false));

        // --- IDENTIFIER (después de keywords) ---
        rules.add(new TokenRule(new Regex("\\G[A-Za-z_][A-Za-z_0-9]*"), types.IdentifierType.INSTANCE, false));

        return TokenProvider.Companion.fromRules(rules);
    }

    private List<ASTNode> parseAll(List<Token> tokens) {
        List<ParserRule> rules = Arrays.asList(
                new PrintlnRule(new PrintBuilder()),
                new VariableDeclarationRule(new VariableDeclarationBuilder()),
                new AssignmentRule(new AssignmentBuilder()),
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