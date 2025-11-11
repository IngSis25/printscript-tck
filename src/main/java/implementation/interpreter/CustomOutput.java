package implementation.interpreter;

import interpreter.PrintEmitter;
import org.example.output.Output;
import org.jetbrains.annotations.NotNull;

public class CustomOutput implements Output {

    private final PrintEmitter printer;

    public CustomOutput(PrintEmitter printer) {
        this.printer = printer;
    }

    @Override
    public void write(@NotNull String s) {
        try {
            String cleanMsg = s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
            printer.print(cleanMsg);
        } catch (OutOfMemoryError e) {
            // Re-lanzar para que sea capturado por MyPrintScriptInterpreter
            throw e;
        }
    }
}

