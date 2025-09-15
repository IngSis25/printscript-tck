package implementation;

import com.google.gson.*;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import kotlin.text.Regex;
import main.kotlin.analyzer.Analyzer;
import main.kotlin.analyzer.AnalysisResult;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.DefaultAnalyzer;
import main.kotlin.analyzer.IdentifierFormat;
import main.kotlin.analyzer.IdentifierFormatConfig;
import main.kotlin.analyzer.PrintlnRestrictionConfig;
import main.kotlin.lexer.*;
import org.example.LiteralNumber;
import org.example.LiteralString;
import org.example.ast.ASTNode;
import parser.rules.ParserRule;
import rules.*;
import builders.ExpressionBuilder;
import builders.PrintBuilder;
import builders.VariableDeclarationBuilder;
import main.kotlin.parser.ParseResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LinterAdapter implements PrintScriptLinter {
  private final Loader loader = new Loader();

  @Override
  public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
    try {
      // 1) lex+parse
      Lexer lexer = new DefaultLexer(defaultTokenProvider());
      String code = readAll(src);
      List<Token> tokens = lexer.tokenize(code);
      List<ASTNode> ast = parseAll(tokens);

      // 2) config (tolerante: plano o anidado; con defaults sensatos)
      AnalyzerConfig cfg = parseAnalyzerConfigTolerant(config);

      // 3) analyze
      Analyzer analyzer = new DefaultAnalyzer();
      AnalysisResult result = analyzer.analyze(ast, cfg);
      result.getDiagnostics().forEach(d -> handler.reportError(d.getMessage()));
    } catch (Throwable t) {
      handler.reportError(t.getMessage() == null ? t.toString() : t.getMessage());
    }
  }

  // ------------------ CONFIG PARSE TOLERANTE (PLANO / ANIDADO) ------------------

  private AnalyzerConfig parseAnalyzerConfigTolerant(InputStream configStream) {
    if (configStream == null) return defaultConfig();

    String json;
    try (Reader r = loader.streamToReader(configStream)) {
      json = readAll(r);
    } catch (Exception e) {
      return defaultConfig();
    }

    JsonElement root;
    try {
      root = JsonParser.parseString(json);
    } catch (Exception e) {
      return defaultConfig();
    }
    if (!root.isJsonObject()) return defaultConfig();

    JsonObject obj = root.getAsJsonObject();

    // -------- meta --------
    int maxErrors = getInt(obj, "maxErrors", getInt(obj, "max_errors", 100));
    boolean enableWarnings = getBool(obj, "enableWarnings", getBool(obj, "enable_warnings", true));
    boolean strictMode = getBool(obj, "strictMode", getBool(obj, "strict_mode", false));

    // -------- identifierFormat: acepta plano ("snake case") o anidado { enabled, format } --------
    boolean idEnabled = false; // default
    IdentifierFormat idFmt = IdentifierFormat.CAMEL_CASE; // default

    JsonElement idRaw = pick(obj, "identifierFormat", "identifier_format");
    if (idRaw != null && !idRaw.isJsonNull()) {
      if (idRaw.isJsonObject()) {
        JsonObject io = idRaw.getAsJsonObject();
        if (io.has("enabled") && io.get("enabled").isJsonPrimitive()) {
          idEnabled = io.get("enabled").getAsBoolean();
        }
        if (io.has("format") && io.get("format").isJsonPrimitive()) {
          idFmt = parseFmt(io.get("format").getAsString());
        }
      } else if (idRaw.isJsonPrimitive() && idRaw.getAsJsonPrimitive().isString()) {
        // forma plana: "camel case" | "snake case" | "CAMEL_CASE" | "SNAKE_CASE"
        idFmt = parseFmt(idRaw.getAsString());
        idEnabled = true;
      }
    }

    // -------- printlnRestrictions: acepta booleano plano o anidado { enabled, allowOnly... } --------
    boolean prEnabled = true; // default
    boolean allowOnly = true; // default (solo literal/identificador)

    JsonElement prRaw = pick(obj, "printlnRestrictions", "println_restrictions");
    if (prRaw != null && !prRaw.isJsonNull()) {
      if (prRaw.isJsonPrimitive() && prRaw.getAsJsonPrimitive().isBoolean()) {
        prEnabled = prRaw.getAsBoolean();
      } else if (prRaw.isJsonObject()) {
        JsonObject po = prRaw.getAsJsonObject();
        if (po.has("enabled") && po.get("enabled").isJsonPrimitive()) {
          prEnabled = po.get("enabled").getAsBoolean();
        }
        if (po.has("allowOnlyIdentifiersAndLiterals") && po.get("allowOnlyIdentifiersAndLiterals").isJsonPrimitive()) {
          allowOnly = po.get("allowOnlyIdentifiersAndLiterals").getAsBoolean();
        }
        if (po.has("allow_only_identifiers_and_literals") && po.get("allow_only_identifiers_and_literals").isJsonPrimitive()) {
          allowOnly = po.get("allow_only_identifiers_and_literals").getAsBoolean();
        }
        if (po.has("mandatory-variable-or-literal-in-println") && po.get("mandatory-variable-or-literal-in-println").isJsonPrimitive()) {
          allowOnly = po.get("mandatory-variable-or-literal-in-println").getAsBoolean();
        }
      }
    }

    return new AnalyzerConfig(
        new IdentifierFormatConfig(idEnabled, idFmt),
        new PrintlnRestrictionConfig(prEnabled, allowOnly),
        maxErrors,
        enableWarnings,
        strictMode
    );
  }

  private static JsonElement pick(JsonObject o, String a, String b) {
    if (o.has(a)) return o.get(a);
    if (o.has(b)) return o.get(b);
    return null;
  }

  private static int getInt(JsonObject o, String k, int def) {
    return (o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsInt() : def;
  }

  private static boolean getBool(JsonObject o, String k, boolean def) {
    return (o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsBoolean() : def;
  }

  private static IdentifierFormat parseFmt(String raw) {
    if (raw == null) return IdentifierFormat.CAMEL_CASE;
    String norm = raw.trim()
        .replace('-', ' ')
        .replace('_', ' ')
        .replaceAll("\\s+", " ")
        .toUpperCase();
    if (norm.equals("CAMEL CASE") || norm.equals("CAMELCASE") || norm.equals("CAMEL")) {
      return IdentifierFormat.CAMEL_CASE;
    }
    if (norm.equals("SNAKE CASE") || norm.equals("SNAKECASE") || norm.equals("SNAKE")) {
      return IdentifierFormat.SNAKE_CASE;
    }
    try { return IdentifierFormat.valueOf(raw.trim().toUpperCase().replace(' ', '_')); }
    catch (IllegalArgumentException ignored) { return IdentifierFormat.CAMEL_CASE; }
  }

  private static AnalyzerConfig defaultConfig() {
    return new AnalyzerConfig(
        new IdentifierFormatConfig(false, IdentifierFormat.CAMEL_CASE), // <--- false acá
        new PrintlnRestrictionConfig(true, true),
        100,
        true,
        false
    );
  }

  private static String readAll(Reader in) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[4096];
    int n;
    while ((n = in.read(buf)) != -1) sb.append(buf, 0, n);
    return sb.toString();
  }

  // ------------------ LÉXICO/PARSER (igual que tu versión) ------------------

  private TokenProvider defaultTokenProvider() {
    List<TokenRule> rules = new ArrayList<>();
    rules.add(new TokenRule(new Regex("\\G[ \\t]+"), types.WhitespaceType.INSTANCE, true));
    rules.add(new TokenRule(new Regex("\\G(?:\\r?\\n)+"), types.WhitespaceType.INSTANCE, true));
    rules.add(new TokenRule(new Regex("\\G\\bprintln\\b"), types.PrintlnType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\bnumber\\b"), types.NumberType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\bstring\\b"), types.StringType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\b(?:const|let|var)\\b"), types.ModifierType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G:"), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G;"), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\("), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\\)"), types.PunctuationType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G(?:==|!=|<=|>=)"), types.OperatorType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G="), types.AssignmentType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G[+\\-*/<>]"), types.OperatorType.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G\"([^\"\\\\]|\\\\.)*\""), LiteralString.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G'([^'\\\\]|\\\\.)*'"),  LiteralString.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G[0-9]+(?:\\.[0-9]+)?"), LiteralNumber.INSTANCE, false));
    rules.add(new TokenRule(new Regex("\\G[A-Za-z_][A-Za-z_0-9]*"), types.IdentifierType.INSTANCE, false));
    return TokenProvider.Companion.fromRules(rules);
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
