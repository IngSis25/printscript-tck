package implementation;

import com.google.gson.annotations.SerializedName;
import main.kotlin.analyzer.AnalyzerConfig;
import main.kotlin.analyzer.IdentifierFormat;
import main.kotlin.analyzer.IdentifierFormatConfig;
import main.kotlin.analyzer.PrintlnRestrictionConfig;

/** Maps various TCK config field names to our AnalyzerConfig. */
public class LinterConfigAdapter {
  @SerializedName(value="identifier_format", alternate = {"identifierFormat"})
  public String identifierFormat;
  @SerializedName(value="mandatory-variable-or-literal-in-println", alternate = {"printlnRestrictions.allowOnlyIdentifiersAndLiterals"})
  public Boolean mandatoryVarOrLiteralInPrintln;
  @SerializedName(value="maxErrors", alternate={"max_errors"})
  public Integer maxErrors;
  @SerializedName(value="enableWarnings", alternate={"enable_warnings"})
  public Boolean enableWarnings;
  @SerializedName(value="strictMode", alternate={"strict_mode"})
  public Boolean strictMode;

  public AnalyzerConfig toConfig() {
    IdentifierFormat fmt = IdentifierFormat.CAMEL_CASE;
    if (identifierFormat != null) {
      String s = identifierFormat.trim().toUpperCase();
      try { fmt = IdentifierFormat.valueOf(s); } catch (Exception ignored) {}
    }
    boolean allowOnly = mandatoryVarOrLiteralInPrintln != null ? mandatoryVarOrLiteralInPrintln : true;
    int max = maxErrors != null ? maxErrors : 100;
    boolean warn = enableWarnings != null ? enableWarnings : true;
    boolean strict = strictMode != null ? strictMode : false;

    return new AnalyzerConfig(
        new IdentifierFormatConfig(true, fmt),
        new PrintlnRestrictionConfig(true, allowOnly),
        max,
        warn,
        strict
    );
  }
}
