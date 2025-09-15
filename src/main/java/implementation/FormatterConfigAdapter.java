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
            "enforce-spacing-surrounding-equals", "enforce_spacing_surrounding_equals",
            "enforce-spacing-around-equals", "enforce_spacing_around_equals"
    })
    public Boolean spaceAroundEquals = null;  // ✅ CAMBIO: true → null

    @SerializedName(value = "spaceBeforeColon", alternate = {
            "space_before_colon", "space-before-colon",
            "enforce-spacing-before-colon-in-declaration", "enforce_spacing_before_colon_in_declaration"
    })
    public Boolean spaceBeforeColon = false;

    @SerializedName(value = "spaceAfterColon", alternate = {
            "space_after_colon", "space-after-colon",
            "enforce-spacing-after-colon-in-declaration", "enforce_spacing_after_colon_in_declaration"
    })
    public Boolean spaceAfterColon = false;

    @SerializedName(value = "spaceAroundAssignment", alternate = {
            "space_around_assignment", "space-around-assignment",
            "enforce-spacing-surrounding-assignment", "enforce_spacing_surrounding_assignment"
    })
    public Boolean spaceAroundAssignment = true;

    @SerializedName(value = "noSpaceAroundEquals", alternate = {
            "enforce-no-spacing-around-equals", "enforce_no_spacing_around_equals"
    })
    public Boolean noSpaceAroundEquals = false;

    public FormatterConfig toConfig() {
        // ✅ LÓGICA ESPECIAL DESCUBIERTA:
        boolean finalSpaceAfterColon = (spaceAfterColon != null) ? spaceAfterColon : false;
        boolean finalSpaceAroundAssignment = (spaceAroundAssignment != null) ? spaceAroundAssignment : true;

        // enforce-spacing-around-equals activa TANTO espacios después de : COMO alrededor de =
        if (spaceAroundEquals != null && spaceAroundEquals) {
            finalSpaceAfterColon = true;
            finalSpaceAroundAssignment = true;
        }

        // enforce-no-spacing-around-equals MANTIENE espacios después de : pero quita alrededor de =
        if (noSpaceAroundEquals != null && noSpaceAroundEquals) {
            finalSpaceAfterColon = true;  // ¡MANTIENE el espacio después de :!
            finalSpaceAroundAssignment = false;
        }

        return new FormatterConfig(
                lineBreaksBeforePrints != null ? lineBreaksBeforePrints : 0,
                spaceAroundEquals != null ? spaceAroundEquals : true,
                spaceBeforeColon != null ? spaceBeforeColon : false,
                finalSpaceAfterColon,
                finalSpaceAroundAssignment
        );
    }
}