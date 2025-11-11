import implementation.formatter.MyPrintScriptFormatter;
import java.io.*;

public class TestFormatter {
    public static void main(String[] args) throws Exception {
        String code = "let something: string= \"a really cool thing\";";
        String config = "{\n  \"enforce-no-spacing-around-equals\": true\n}";
        
        MyPrintScriptFormatter formatter = new MyPrintScriptFormatter();
        
        InputStream srcStream = new ByteArrayInputStream(code.getBytes());
        InputStream configStream = new ByteArrayInputStream(config.getBytes());
        StringWriter writer = new StringWriter();
        
        formatter.format(srcStream, "1.0", configStream, writer);
        
        System.out.println("Input:  '" + code + "'");
        System.out.println("Output: '" + writer.toString() + "'");
        System.out.println("Expected: 'let something: string=\"a really cool thing\";'");
    }
}

