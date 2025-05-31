import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private Scanner scanner;

    public CommandManager() {
        commands.put(CommandConstants.ECHO, this::echo);
        commands.put(CommandConstants.TYPE, this::type);
        commands.put(CommandConstants.EXIT, this::exit);
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

        if (CommandConstants.ALL_COMMANDS.contains(commandName)) {
            System.out.println(commandName + " is a shell builtin");
            return;
        }

        System.out.println(commandName + ": not found");

    }

    public void exit(String[] args) {
        scanner.close();
        System.exit(Integer.parseInt(args[0]));
    }


}
