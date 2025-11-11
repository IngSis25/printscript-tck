package implementation.interpreter;

import implementation.util.InputStreamToStringReader;
import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;

import java.io.InputStream;
import java.io.Reader;

import static implementation.util.InputStreamToStringReader.convert;

public class MyPrintScriptInterpreter implements PrintScriptInterpreter {
    
    @Override
    public void interpret(InputStream src, String version, InputProvider input, PrintEmitter output, ErrorHandler handler) {
        execute(src, version, output, handler, input);
    }

    @Override
    public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        try {
            Reader reader = convert(src);
            Runner runner = new Runner(version, reader);
            CustomOutput output = new CustomOutput(emitter);
            CustomInput input = new CustomInput(provider);
            
            runner.execute(version, output, input);
        } catch (OutOfMemoryError e) {
            handler.reportError("Java heap space");
        } catch (Throwable e) {
            handler.reportError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}

