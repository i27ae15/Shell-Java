import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, ReturnableCommand> returnableCommands = new HashMap<>();
    private final Map<String, VoidCommand> voidCommands = new HashMap<>();

    private Scanner scanner;
    private ConsoleState consoleState;

    public CommandManager() {
        returnableCommands.put(CommandConstants.ECHO, this::echo);
        returnableCommands.put(CommandConstants.TYPE, this::type);
        returnableCommands.put(CommandConstants.PWD, this::pwd);

        voidCommands.put(CommandConstants.CD, this::cd);
        voidCommands.put(CommandConstants.EXIT, this::exit);

        consoleState = new ConsoleState();
    }

    public boolean runCommand(Scanner scan, String input) {

        this.scanner = scan;

        String[] commandNameAndCleanedInput = CommandUtils.getCommandAndCleanInput(input);
        String commandName = commandNameAndCleanedInput[0];
        String cleanedInput = commandNameAndCleanedInput[1];

        ArrayList<String> args = CommandUtils.quoterCleaner(cleanedInput);

        VoidCommand voidAction = voidCommands.get(commandName);
        if (voidAction != null) {
            voidAction.execute(args);
            return true;
        }

        String[] filesToRedirect = CommandUtils.getRedirection(args);

        String redirectFrom = filesToRedirect[0];
        String fileToRedirectTo = filesToRedirect[1];
        String redirectionType = filesToRedirect[2];

        StringPair output;
        ReturnableCommand action = returnableCommands.get(commandName);
        if (action != null) {
            output = action.execute(args);
        } else {

            String executableFile = consoleState.findFileOnPath(commandName);
            if (executableFile != null) {
                output = FileUtils.runExternalProgram(commandName, args);
            } else {
                System.out.println(commandName + ": command not found");
                return false;
            }

        }

        ContextManager.outPutManager(output, redirectFrom, fileToRedirectTo, redirectionType);
        return true;

    }

    private void cat(List<String> input, String fileToRedirectTo, boolean redirectError) {
        boolean redirect = (fileToRedirectTo != null && !fileToRedirectTo.isEmpty());
        ArrayList<String> toBreak = new ArrayList<>();

        toBreak.add(">");
        toBreak.add("1>");
        toBreak.add("2>");


        for (String fileName : input) {

            boolean isError = false;
            if (toBreak.contains(fileName)) break;

            String output;
            Path path = Path.of(fileName);
            boolean fileExists = Files.exists(path);

            if (fileExists) {
                try {
                    output = Files.readString(path);
                } catch (IOException e) {
                    output = "cat: " + fileName + ": I/O error\n";
                }

            } else {

                output = "cat: " + fileName + ": No such file or directory";
                isError = true;

                if (!redirectError) {
                    System.out.println(output);
                    continue;
                } else {
                    output += '\n';
                }

            }

            if (redirect && (!redirectError && !isError || redirectError && isError)) {
                try {
                    FileUtils.writeToFile(output, fileToRedirectTo);
                } catch (Exception e) {
                    System.out.println("Failed to write: " + e.getMessage());
                }
            } else {
                System.out.print(output);
                if (!output.isEmpty() && output.charAt(output.length() - 1) != '\n') {
                   System.out.println();
                }
            }
        }
    }

    private StringPair echo(ArrayList<String> args) {

        StringBuilder output = new StringBuilder();
        String error = "";

        // if (fileToRedirectTo != null && !fileToRedirectTo.isEmpty()) {
        //     output = redirectFrom;

        //     try {
        //         if (isError) {
        //             FileWriter.writeToFile("", fileToRedirectTo);
        //             System.out.println(redirectFrom);
        //         } else {
        //             FileWriter.writeToFile(redirectFrom + "\n", fileToRedirectTo);
        //         }
        //     } catch (Exception e) {
        //         error = "Failed to run program: " + e.getMessage();
        //     }

        // }

        for (String arg : args) {
            if (CommandUtils.REDIRECT_TOKENS.contains(arg)) break;
            output.append(arg).append(" ");
        }

        return new StringPair(output.toString(), error);
    }

    private StringPair pwd(ArrayList<String> args) {
        return new StringPair(consoleState.getCWD(), "");
    }

    private StringPair type(ArrayList<String> args) {
        final String commandName = args.get(0);

        if (CommandConstants.ALL_COMMANDS.contains(commandName)) {
            String output = String.format("%s is a shell builtin", commandName);
            return new StringPair(output, "");
        }

        final String executableFile = consoleState.findFileOnPath(commandName);
        if (executableFile != null) {
            String output = String.format("%s is %s", commandName, executableFile);
            return new StringPair(output, "");
        }

        String error = String.format("%s: not found", commandName);
        return new StringPair("", error);
    }

    // NON_RETURNABLE COMMANDS

    private void cd(ArrayList<String> args) {

        String path = args.get(0);

        // Check if the user wants to go back
        if (path.startsWith("~")) {
            consoleState.setCurrentDir(System.getenv("HOME"));
            return;
        }

        if (path.startsWith("~")) {
            consoleState.setCurrentDir(System.getenv("HOME"));
            return;
        }
        if (consoleState.goBack(path)) return;
        if (path.startsWith("~")) {
            consoleState.setCurrentDir(System.getenv("HOME"));
            return;
        }


        if (path.startsWith("./")) {
            // delete the .
            path = path.substring(1, path.length());

            // Add the cwd at front
            path = consoleState.getCWD() + path;
        }

        // Check for absolute path
        File dir = new File(path);

        if (dir.exists() && dir.isDirectory()) {
            consoleState.setCurrentDir(path);
            return;
        }

        System.out.println("cd: " + path + ": No such file or directory");
    }

    private void exit(ArrayList<String> args) {
        scanner.close();
        System.exit(Integer.parseInt(args.get(0)));
    }

}
