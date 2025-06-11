import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        CommandManager commandManager = new CommandManager();

        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine();
            commandManager.runCommand(scanner, input);

        }


    }

}
