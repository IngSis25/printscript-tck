package implementation;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.IdentifierFormat;
import main.kotlin.analyzer.IdentifierFormatConfig;
import main.kotlin.analyzer.PrintlnRestrictionConfig;

/**
 * Adapter tolerante: acepta JSON anidado o plano.
 *
 * Soporta:
 *   1) { "identifier_format": "camel case" }
 *   2) { "identifierFormat": { "enabled": true, "format": "SNAKE_CASE" } }
 *   3) { "printlnRestrictions": true }  // habilita con defaults
 *   4) { "printlnRestrictions": { "enabled": false, "allowOnlyIdentifiersAndLiterals": true } }
 */
public class LinterConfigAdapter {

  // Usamos JsonElement para poder aceptar objeto / string / boolean sin colisiones
  @SerializedName(value = "identifierFormat", alternate = {"identifier_format"})
  public JsonElement identifierFormatRaw;

  @SerializedName(value = "printlnRestrictions", alternate = {"println_restrictions"})
  public JsonElement printlnRestrictionsRaw;

  // Meta
  @SerializedName(value = "maxErrors", alternate = {"max_errors"})
  public Integer maxErrors;

  @SerializedName(value = "enableWarnings", alternate = {"enable_warnings"})
  public Boolean enableWarnings;

  @SerializedName(value = "strictMode", alternate = {"strict_mode"})
  public Boolean strictMode;

  public AnalyzerConfig toConfig() {
    // ----- identifierFormat -----
    boolean idEnabled = true; // default
    IdentifierFormat idFmt = IdentifierFormat.CAMEL_CASE; // default

    if (identifierFormatRaw != null && !identifierFormatRaw.isJsonNull()) {
      if (identifierFormatRaw.isJsonPrimitive() && identifierFormatRaw.getAsJsonPrimitive().isString()) {
        // Forma plana: "camel case" | "snake case" | "CAMEL_CASE" | "SNAKE_CASE"
        idFmt = parseFmt(identifierFormatRaw.getAsString());
        idEnabled = true; // por defecto activada si el usuario especifica formato plano
      } else if (identifierFormatRaw.isJsonObject()) {
        var obj = identifierFormatRaw.getAsJsonObject();
        if (obj.has("enabled") && !obj.get("enabled").isJsonNull()) {
          idEnabled = obj.get("enabled").getAsBoolean();
        }
        if (obj.has("format") && !obj.get("format").isJsonNull()) {
          idFmt = parseFmt(obj.get("format").getAsString());
        }
      }
    }

    // ----- printlnRestrictions -----
    boolean prEnabled = true;          // default
    boolean allowOnly = true;          // default (solo literal/identificador)

    if (printlnRestrictionsRaw != null && !printlnRestrictionsRaw.isJsonNull()) {
      if (printlnRestrictionsRaw.isJsonPrimitive()) {
        var prim = printlnRestrictionsRaw.getAsJsonPrimitive();
        if (prim.isBoolean()) {
          prEnabled = prim.getAsBoolean();
          // keep allowOnly default si es boolean plano
        }
      } else if (printlnRestrictionsRaw.isJsonObject()) {
        var obj = printlnRestrictionsRaw.getAsJsonObject();
        if (obj.has("enabled") && !obj.get("enabled").isJsonNull()) {
          prEnabled = obj.get("enabled").getAsBoolean();
        }
        if (obj.has("allowOnlyIdentifiersAndLiterals") && !obj.get("allowOnlyIdentifiersAndLiterals").isJsonNull()) {
          allowOnly = obj.get("allowOnlyIdentifiersAndLiterals").getAsBoolean();
        }
        // Aliases opcionales si tu TCK los usa:
        if (obj.has("allow_only_identifiers_and_literals") && !obj.get("allow_only_identifiers_and_literals").isJsonNull()) {
          allowOnly = obj.get("allow_only_identifiers_and_literals").getAsBoolean();
        }
        if (obj.has("mandatory-variable-or-literal-in-println") && !obj.get("mandatory-variable-or-literal-in-println").isJsonNull()) {
          allowOnly = obj.get("mandatory-variable-or-literal-in-println").getAsBoolean();
        }
      }
    }

    // ----- meta -----
    int max = (maxErrors != null) ? maxErrors : 100;
    boolean warn = (enableWarnings != null) ? enableWarnings : true;
    boolean strict = (strictMode != null) ? strictMode : false;

    return new AnalyzerConfig(
        new IdentifierFormatConfig(idEnabled, idFmt),
        new PrintlnRestrictionConfig(prEnabled, allowOnly),
        max,
        warn,
        strict
    );
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
    // Si ya vino en "CAMEL_CASE" / "SNAKE_CASE"
    try {
      return IdentifierFormat.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
    } catch (IllegalArgumentException ignored) {
      return IdentifierFormat.CAMEL_CASE; // fallback seguro
    }
  }
}
