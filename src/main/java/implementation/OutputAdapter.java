package implementation;

import interpreter.PrintEmitter;
import org.example.output.Output;

/** Bridges our Output interface to the TCK's PrintEmitter. */
public class OutputAdapter implements Output {
  private final PrintEmitter emitter;
  public OutputAdapter(PrintEmitter emitter) { this.emitter = emitter; }
  @Override public void write(String msg) { emitter.print(msg); }
}
