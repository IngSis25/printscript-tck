package implementation.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class InputStreamToStringReader {
    public static Reader convert(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream));
    }
}

