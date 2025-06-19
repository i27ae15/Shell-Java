import java.util.ArrayList;

@FunctionalInterface
public interface ReturnableCommand {
    StringPair execute(ArrayList<String> args);
}
