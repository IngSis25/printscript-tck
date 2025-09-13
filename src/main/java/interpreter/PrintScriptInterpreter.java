package interpreter;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

public interface PrintScriptInterpreter {
    void interpret(InputStream src, String version, InputProvider input, PrintEmitter output, ErrorHandler handler);

    /**
     * executes a PrintScript file handling its resulting messages and errors.
     * @param src Source file.
     * @param version PrintScript version, 1.0 and 1.1 must be supported.
     * @param emitter interface where print statements must be called.
     * @param handler interface where all syntax and semantic error will be reported.
     * @param provider interface that provides input values during the execution.
     */
    void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider);
}
