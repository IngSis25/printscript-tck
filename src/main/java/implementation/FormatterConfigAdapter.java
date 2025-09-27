package implementation;

import com.google.gson.annotations.SerializedName;
import org.example.formatter.config.FormatterConfig;

/**
 * Mapea JSON/YAML del TCK al FormatterConfig real.
 * No reimplementa reglas del formateador; solo setea flags/valores.
 */
public class FormatterConfigAdapter {

    @SerializedName(value = "space_before_colon",  alternate = {"spaceBeforeColon"})
    public Boolean spaceBeforeColon;

    @SerializedName(value = "space_after_colon",   alternate = {"spaceAfterColon"})
    public Boolean spaceAfterColon;

    @SerializedName(value = "space_before_equal",  alternate = {"spaceBeforeEqual"})
    public Boolean spaceBeforeEqual;

    @SerializedName(value = "space_after_equal",   alternate = {"spaceAfterEqual"})
    public Boolean spaceAfterEqual;

    // 0, 1 o 2 (según consigna). Si viene null, dejamos 0 (sin salto extra).
    @SerializedName(value = "newline_before_print", alternate = {"newlinesBeforePrint"})
    public Integer newlinesBeforePrint;

    public FormatterConfig toConfig() {
        return new FormatterConfig(
        );
    }
}
