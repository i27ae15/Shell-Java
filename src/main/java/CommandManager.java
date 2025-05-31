import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private Scanner scanner;

    public CommandManager() {
        commands.put(CommandConstants.ECHO, this::echo);
        commands.put(CommandConstants.EXIT_0, this::exit);
    }

    public boolean runCommand(Scanner scan, String input) {

        this.scanner = scan;

        String commandName = input.toLowerCase();
        Command action = commands.get(commandName.toLowerCase());

        if (action != null) {
            action.execute(commandName);
            return true;
        }

        System.out.println(commandName + ": command not found");
        return false;
    }

    public void echo(String command) {
        System.out.println(command);
    }

    public void exit(String command) {
        System.out.println(CommandConstants.EXIT_0);
        scanner.close();
        System.exit(0);
    }


}
