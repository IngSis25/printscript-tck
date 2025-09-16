package implementation;

import com.google.gson.annotations.SerializedName;
import org.example.formatter.config.FormatterConfig;

public class FormatterConfigAdapter {
    @SerializedName(value = "indentSize", alternate = {
            "indent_size", "indent-size", "indentation", "number-of-indentation-spaces"
    })
    public Integer indentSize = 2;

    @SerializedName(value = "ifBraceSameLine", alternate = {
            "if_brace_same_line", "if-brace-same-line", "brace_same_line", "brace-in-same-line"
    })
    public Boolean ifBraceSameLine = true;

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
    public Boolean spaceAroundEquals = null;

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

    @SerializedName(value = "mandatoryLineBreakAfterStatement", alternate = {
            "mandatory-line-break-after-statement", "mandatory_line_break_after_statement"
    })
    public Boolean mandatoryLineBreakAfterStatement = false;

    @SerializedName(value = "mandatorySingleSpaceSeparation", alternate = {
            "mandatory-single-space-separation", "mandatory_single_space_separation"
    })
    public Boolean mandatorySingleSpaceSeparation = false;

    @SerializedName(value = "mandatorySpaceSurroundingOperations", alternate = {
            "mandatory-space-surrounding-operations", "mandatory_space_surrounding_operations"
    })
    public Boolean mandatorySpaceSurroundingOperations = false;

    @SerializedName(value = "spaceInsideParentheses", alternate = {
            "space_inside_parentheses", "space-inside-parentheses"
    })
    public Boolean spaceInsideParentheses = false;

    public FormatterConfig toConfig() {
        boolean finalSpaceBeforeColon = (spaceBeforeColon != null) ? spaceBeforeColon : false;
        boolean finalSpaceAfterColon  = (spaceAfterColon  != null) ? spaceAfterColon  : false;
        boolean finalSpaceAroundAssignment = (spaceAroundAssignment != null) ? spaceAroundAssignment : true;
        boolean finalSpaceInsideParentheses = (spaceInsideParentheses != null) ? spaceInsideParentheses : false;

        // MANDATORY SINGLE SPACE SEPARATION - activa espacios en : y ()
        if (mandatorySingleSpaceSeparation != null && mandatorySingleSpaceSeparation) {
            finalSpaceBeforeColon = true;
            finalSpaceAfterColon = true;
            finalSpaceInsideParentheses = true;  // ✅ También activa espacios en paréntesis
        }

        // MANDATORY SPACE SURROUNDING OPERATIONS - activa espacio después de :
        if (mandatorySpaceSurroundingOperations != null && mandatorySpaceSurroundingOperations) {
            finalSpaceAfterColon = true;
        }

        // Si piden espacios alrededor de "="
        if (spaceAroundEquals != null && spaceAroundEquals) {
            finalSpaceAfterColon = true;
            finalSpaceAroundAssignment = true;
        }

        // Si piden NO espacios alrededor de "="
        if (noSpaceAroundEquals != null && noSpaceAroundEquals) {
            finalSpaceAfterColon = true;
            finalSpaceAroundAssignment = false;
        }

        return new FormatterConfig(
                (lineBreaksBeforePrints != null) ? lineBreaksBeforePrints : 0,
                (spaceAroundEquals != null) ? spaceAroundEquals : true,
                finalSpaceBeforeColon,
                finalSpaceAfterColon,
                finalSpaceAroundAssignment,
                finalSpaceInsideParentheses,
                (indentSize != null) ? indentSize : 2,
                (ifBraceSameLine != null) ? ifBraceSameLine : true
        );
    }
}