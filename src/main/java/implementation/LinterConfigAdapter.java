package implementation;

import com.google.gson.annotations.SerializedName;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.IdentifierFormat;
import main.kotlin.analyzer.IdentifierFormatConfig;
import main.kotlin.analyzer.PrintlnRestrictionConfig;

/**
 * Adapter de configuración del Linter.
 * Solo mapea JSON/YAML -> AnalyzerConfig del core (sin lógica del lenguaje).
 */
public class LinterConfigAdapter {

  // camelCase / snake_case (acepta alias frecuentes)
  @SerializedName(value = "identifier_format", alternate = {"identifierFormat"})
  public String identifierFormat;

  // true => println solo con literal o identificador (no expresiones)
  @SerializedName(
      value = "mandatory-variable-or-literal-in-println",
      alternate = {"printlnRestrictions.allowOnlyIdentifiersAndLiterals"}
  )
  public Boolean mandatoryVarOrLiteralInPrintln;

  // Flags opcionales (dejá solo los que existan en tu AnalyzerConfig)
  @SerializedName(value = "maxErrors", alternate = {"max_errors"})
  public Integer maxErrors;

  @SerializedName(value = "enableWarnings", alternate = {"enable_warnings"})
  public Boolean enableWarnings;

  @SerializedName(value = "strictMode", alternate = {"strict_mode"})
  public Boolean strictMode;

  /** Traduce el DTO a tu config real. */
  public AnalyzerConfig toAnalyzerConfig() {
    IdentifierFormat fmt = parseIdentifierFormatOrDefault(identifierFormat, IdentifierFormat.CAMEL_CASE);

    return new AnalyzerConfig(
        new IdentifierFormatConfig(),
        new PrintlnRestrictionConfig(),
        maxErrors == null ? Integer.MAX_VALUE : maxErrors,
        Boolean.TRUE.equals(enableWarnings),
        Boolean.TRUE.equals(strictMode)
    );
  }

  private static IdentifierFormat parseIdentifierFormatOrDefault(String raw, IdentifierFormat defaultFmt) {
    if (raw == null) return defaultFmt;
    String s = raw.trim().toLowerCase();
    if (s.equals("camelcase") || s.equals("camel_case") || s.equals("camel")) {
      return IdentifierFormat.CAMEL_CASE;
    }
    if (s.equals("snakecase") || s.equals("snake_case") || s.equals("snake")) {
      return IdentifierFormat.SNAKE_CASE;
    }
    return defaultFmt; // validación/errores los maneja el core
  }
}
