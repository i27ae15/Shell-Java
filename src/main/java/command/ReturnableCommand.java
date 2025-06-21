package command;

import java.util.ArrayList;
import utils.StringPair;

@FunctionalInterface
public interface ReturnableCommand {
    StringPair execute(ArrayList<String> args);
}
