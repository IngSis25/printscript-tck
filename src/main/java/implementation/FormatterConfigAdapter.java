package implementation;

import com.google.gson.annotations.SerializedName;
import org.example.formatter.config.FormatterConfig;

/** Accepts multiple naming styles from the TCK config and adapts to our FormatterConfig. */
public class FormatterConfigAdapter {
  @SerializedName(value = "lineBreaksBeforePrints", alternate = {"line_breaks_before_prints","line-breaks-before-prints"})
  public Integer lineBreaksBeforePrints = 0;
  @SerializedName(value = "spaceAroundEquals", alternate = {"space_around_equals","space-around-equals"})
  public Boolean spaceAroundEquals = true;
  @SerializedName(value = "spaceBeforeColon", alternate = {"space_before_colon","space-before-colon"})
  public Boolean spaceBeforeColon = false;
  @SerializedName(value = "spaceAfterColon", alternate = {"space_after_colon","space-after-colon"})
  public Boolean spaceAfterColon = true;

  public FormatterConfig toConfig() {
    return new FormatterConfig(
        lineBreaksBeforePrints != null ? lineBreaksBeforePrints : 0,
        spaceAroundEquals != null ? spaceAroundEquals : Boolean.TRUE,
        spaceBeforeColon != null ? spaceBeforeColon : Boolean.FALSE,
        spaceAfterColon != null ? spaceAfterColon : Boolean.TRUE
    );
  }
}
