package implementation;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

public interface PrintScriptFactory {
    PrintScriptInterpreter interpreter();
    PrintScriptFormatter formatter();
    PrintScriptLinter linter();
}
