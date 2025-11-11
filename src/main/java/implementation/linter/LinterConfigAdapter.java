package implementation.linter;

import com.google.gson.annotations.SerializedName;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.IdentifierFormat;
import main.kotlin.analyzer.IdentifierFormatConfig;
import main.kotlin.analyzer.PrintlnRestrictionConfig;

/**
 * Adapter de configuración del Linter.
 * Mapea JSON (anidado o plano) -> AnalyzerConfig del core.
 */
public class LinterConfigAdapter {

    // -------- Variante LEGACY (plana) --------
    // "identifier_format": "snake case" | "camel case" | "SNAKE_CASE" | "CAMEL_CASE"
    @SerializedName(value = "identifier_format", alternate = {"identifierFormatString"})
    public String identifierFormatFlat;

    // true => println solo con literal o identificador (no expresiones)
    @SerializedName(value = "mandatory-variable-or-literal-in-println")
    public Boolean mandatoryVarOrLiteralInPrintlnFlat;

    // -------- Variante NUEVA (anidada) --------
    @SerializedName("identifierFormat")
    public IdentifierFormatDTO identifierFormat;

    @SerializedName("printlnRestrictions")
    public PrintlnRestrictionsDTO printlnRestrictions;

    // -------- Flags opcionales del core --------
    @SerializedName(value = "maxErrors", alternate = {"max_errors"})
    public Integer maxErrors;

    @SerializedName(value = "enableWarnings", alternate = {"enable_warnings"})
    public Boolean enableWarnings;

    @SerializedName(value = "strictMode", alternate = {"strict_mode"})
    public Boolean strictMode;

    /** Traduce el DTO a tu config real. */
    public AnalyzerConfig toAnalyzerConfig(com.google.gson.JsonObject jsonConfig) {
        boolean idEnabled;
        IdentifierFormat idFormat;

        if (identifierFormat != null) {
            idEnabled = Boolean.TRUE.equals(identifierFormat.enabled);
            idFormat  = parseIdentifierFormatOrDefault(identifierFormat.format, IdentifierFormat.CAMEL_CASE);
        } else {
            // Si no hay config, desactivar la regla
            idEnabled = identifierFormatFlat != null && !identifierFormatFlat.trim().isEmpty();
            idFormat  = parseIdentifierFormatOrDefault(identifierFormatFlat, IdentifierFormat.CAMEL_CASE);
        }

        boolean prEnabled;
        boolean allowOnlyIdOrLit;

        if (printlnRestrictions != null) {
            prEnabled        = Boolean.TRUE.equals(printlnRestrictions.enabled);
            allowOnlyIdOrLit = Boolean.TRUE.equals(printlnRestrictions.allowOnlyIdentifiersAndLiterals);
        } else {
            // Si no hay config, desactivar la regla
            prEnabled        = mandatoryVarOrLiteralInPrintlnFlat != null && mandatoryVarOrLiteralInPrintlnFlat;
            allowOnlyIdOrLit = Boolean.TRUE.equals(mandatoryVarOrLiteralInPrintlnFlat);
        }

        int maxErr      = (maxErrors == null) ? 100 : maxErrors;
        // Si enableWarnings no está definido, desactivar por defecto
        boolean warnOn  = Boolean.TRUE.equals(enableWarnings);
        boolean strict  = Boolean.TRUE.equals(strictMode);
        IdentifierFormatConfig idCfg =
                new IdentifierFormatConfig(idEnabled, idFormat);
        PrintlnRestrictionConfig prCfg =
                new PrintlnRestrictionConfig(prEnabled, allowOnlyIdOrLit);

        // Si jsonConfig es null, usar el jsonConfig que necesita tu DefaultAnalyzer
        // para que funcione sin visitors por defecto cuando no hay configuración
        return new AnalyzerConfig(idCfg, prCfg, maxErr, warnOn, strict, jsonConfig != null ? jsonConfig : new com.google.gson.JsonObject());
    }

    private static IdentifierFormat parseIdentifierFormatOrDefault(String raw, IdentifierFormat def) {
        if (raw == null) return def;
        String s = raw.trim().toLowerCase();

        if (s.equals("camelcase") || s.equals("camel_case") || s.equals("camel") || s.equals("camel case") || s.equals("camel-case") || s.equals("camelcase()") || s.equals("camel_case()")) {
            return IdentifierFormat.CAMEL_CASE;
        }
        if (s.equals("snakecase") || s.equals("snake_case") || s.equals("snake") || s.equals("snake case") || s.equals("snake-case") || s.equals("snakecase()") || s.equals("snake_case()")) {
            return IdentifierFormat.SNAKE_CASE;
        }

        try {
            return IdentifierFormat.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }


    public static class IdentifierFormatDTO {
        @SerializedName("enabled")
        public Boolean enabled;

        @SerializedName(value = "format")
        public String format;
    }

    public static class PrintlnRestrictionsDTO {
        @SerializedName("enabled")
        public Boolean enabled;

        @SerializedName("allowOnlyIdentifiersAndLiterals")
        public Boolean allowOnlyIdentifiersAndLiterals;
    }
}
