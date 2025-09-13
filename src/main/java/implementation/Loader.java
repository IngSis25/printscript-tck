package implementation;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Small helper to turn InputStreams into reusable Readers/Files. */
public class Loader {
  public File loadFile(InputStream stream) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      File file = File.createTempFile("ps-config", ".json");
      file.deleteOnExit();
      try (OutputStream output = new FileOutputStream(file)) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.write(line.getBytes(StandardCharsets.UTF_8));
          output.write('\n');
        }
      }
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Reader getReader(File f) {
    try {
      return new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Reader streamToReader(InputStream is) {
    return new InputStreamReader(is, StandardCharsets.UTF_8);
  }
}
