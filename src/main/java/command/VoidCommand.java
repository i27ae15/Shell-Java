package command;

import java.util.ArrayList;

@FunctionalInterface
public interface VoidCommand {
    void execute(ArrayList<String> args);
}