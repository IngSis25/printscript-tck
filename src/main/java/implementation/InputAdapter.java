package implementation;

import interpreter.InputProvider;

/** Minimal adapter so we can accept an InputProvider, even if we don't use it yet. */
public class InputAdapter {
  private final InputProvider input;
  public InputAdapter(InputProvider input) { this.input = input; }
  public InputProvider get() { return input; }
}
