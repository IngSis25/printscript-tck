package implementation;

import com.google.gson.annotations.SerializedName;
import org.example.formatter.config.FormatterConfig;

/** Accepts multiple naming styles from the TCK config and adapts to our FormatterConfig. */
public class FormatterConfigAdapter {
  @SerializedName(value = "lineBreaksBeforePrints", alternate = {
    "line_breaks_before_prints", "line-breaks-before-prints", 
    "lineBreaksAfterPrints", "line_breaks_after_prints", "line-breaks-after-println"
  })
  public Integer lineBreaksBeforePrints = 0;
  
  @SerializedName(value = "spaceAroundEquals", alternate = {
    "space_around_equals", "space-around-equals", 
    "enforce-spacing-surrounding-equals", "enforce_spacing_surrounding_equals"
  })
  public Boolean spaceAroundEquals = true;
  
  @SerializedName(value = "spaceBeforeColon", alternate = {
    "space_before_colon", "space-before-colon", 
    "enforce-spacing-before-colon-in-declaration", "enforce_spacing_before_colon_in_declaration"
  })
  public Boolean spaceBeforeColon = false;
  
  @SerializedName(value = "spaceAfterColon", alternate = {
    "space_after_colon", "space-after-colon", 
    "enforce-spacing-after-colon-in-declaration", "enforce_spacing_after_colon_in_declaration"
  })
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
