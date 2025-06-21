package command;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class CommandConstants {

    public static final String ECHO = "echo";
    public static final String TYPE = "type";
    public static final String EXIT = "exit";
    public static final String PWD = "pwd";
    public static final String CD = "cd";
    public static final String CAT = "cat";

    public static final Set<String> ALL_COMMANDS = Collections.unmodifiableSet(new HashSet<>(
        Set.of(ECHO, TYPE, EXIT, PWD, CD)
    ));

}
