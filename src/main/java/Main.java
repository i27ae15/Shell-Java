import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import context.ConsoleState;

public class Main {

    private static void setTerminalRawMode(){
        String[] cmd = {"/bin/sh", "-c", "stty -echo raw </dev/tty"};
        try {
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        context.ConsoleState consoleState = new ConsoleState();
        command.CommandManager commandManager = new command.CommandManager(consoleState, scanner);

        StringBuilder buffer = new StringBuilder();

        setTerminalRawMode();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        utils.Printer.print("$ ");

        while (true) {

            char c = (char) reader.read();

            switch (c) {
                case '\n':
                    utils.Printer.print("\n");
                    String input = buffer.toString();

                    buffer.setLength(0);
                    commandManager.processCommand(input);

                    utils.Printer.print("$ ");
                    break;

                case '\t':
                    String option = consoleState.autocompletionManager(buffer.toString());
                    if (!option.isEmpty()) {
                        buffer.replace(0, buffer.length(), option);
                        utils.Printer.print("$ " + option);
                    }
                    break;

                case '\u001B': // ESC character (27 in decimal)
                    // Read the next two characters to determine which arrow key
                    char bracket = (char) reader.read();
                    char arrow = (char) reader.read();
                    String command = "";

                    if (bracket == '[') {
                        switch (arrow) {
                            case 'A': // Up arrow
                                command = consoleState.getPreviousCommand();
                                break;

                            case 'B': // Down arrow
                                command = consoleState.getNextCommand();
                                break;
                            }

                            System.out.print("\r\033[K"); // \r goes to start, \033[K clears to end of line

                            // Update the buffer with the new command
                            buffer.setLength(0);
                            buffer.append(command);
                            utils.Printer.print ("$ " + command);
                        }

                    break;

                default:
                    buffer.append(c);
                    System.out.print(c);
                    break;
            }

        }


    }

}
