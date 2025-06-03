import java.util.ArrayList;

@FunctionalInterface
public interface Command {
    void execute(ArrayList<String> args);
}
