package implementation.formatter;

import implementation.util.InputStreamToStringReader;
import interpreter.PrintScriptFormatter;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import static implementation.util.InputStreamToStringReader.convert;

public class MyPrintScriptFormatter implements PrintScriptFormatter {

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        try {
            Reader reader = convert(src);
            Runner runner = new Runner(version, reader);

            JsonCreator jsonCreator = new JsonCreator();
            String configJson = jsonCreator.getJsonStringFromInputStream(config);

            String result = runner.format(configJson, version).getFormattedCode();
            writer.write(result);
        } catch (Exception e) {
            throw new RuntimeException("Formatter error: " + e.getMessage(), e);
        }
    }
}

