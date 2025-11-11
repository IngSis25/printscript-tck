package implementation.interpreter;

import interpreter.InputProvider;
import org.example.input.Input;
import org.jetbrains.annotations.NotNull;

public class CustomInput implements Input {
    private final InputProvider provider;

    public CustomInput(InputProvider provider) {
        this.provider = provider;
    }

    @NotNull
    @Override
    public String read(@NotNull String message) {
        return provider.input(message);
    }
}

