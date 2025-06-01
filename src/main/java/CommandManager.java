import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private Scanner scanner;
    private final String strPath;
    private final ArrayList<String> paths = new ArrayList<>();

    public CommandManager() {
        commands.put(CommandConstants.ECHO, this::echo);
        commands.put(CommandConstants.TYPE, this::type);
        commands.put(CommandConstants.EXIT, this::exit);

        this.strPath = System.getenv("PATH");
        Collections.addAll(this.paths, strPath.split(":"));
    }

    public boolean runCommand(Scanner scan, String input) {

        this.scanner = scan;

        String[] tokens = input.trim().split("\\s+");
        String commandName = tokens[0].toLowerCase();

        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);

        Command action = commands.get(commandName);

        if (action != null) {
            action.execute(args);
            return true;
        }

        System.out.println(commandName + ": command not found");
        return false;
    }

    public void echo(String[] args) {
        System.out.println(String.join(" ", args));
    }

    public void type(String[] args) {
        String commandName = args[0];

        // Look on the path first:
        if (CommandConstants.ALL_COMMANDS.contains(commandName)) {
            System.out.println(commandName + " is a shell builtin");
            return;
        }

        // Check each dir in PATH for executable
        for (String dir : paths) {
            File file = new File(dir, commandName);
            if (file.exists() && file.canExecute()) {
                System.out.println(commandName + " is " + file.getAbsolutePath());
                return;
            }
        }

        System.out.println(commandName + ": not found");

    }

    public void exit(String[] args) {
        scanner.close();
        System.exit(Integer.parseInt(args[0]));
    }


    // ----------------------------------------------------------
    // Utils

    public boolean findFolderInPath(String toFind) {
        for (String path : this.paths) {
            if (path.contains(toFind)) return true;
        }

        return false;
    }


}
