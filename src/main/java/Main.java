import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        Scanner scanner = new Scanner(System.in);
        CommandManager commandManager = new CommandManager();

        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine();
            commandManager.runCommand(scanner, input);

        }


    }

}
