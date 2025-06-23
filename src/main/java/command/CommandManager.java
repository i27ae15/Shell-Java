package command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, ReturnableCommand> returnableCommands = new HashMap<>();
    private final Map<String, VoidCommand> voidCommands = new HashMap<>();

    private context.ConsoleState consoleState;
    private Scanner scanner;

    public CommandManager(context.ConsoleState consoleState, Scanner scanner) {
        returnableCommands.put(CommandConstants.ECHO, this::echo);
        returnableCommands.put(CommandConstants.TYPE, this::type);
        returnableCommands.put(CommandConstants.PWD, this::pwd);

        voidCommands.put(CommandConstants.CD, this::cd);
        voidCommands.put(CommandConstants.EXIT, this::exit);

        this.consoleState = consoleState;
        this.scanner = scanner;
    }

    public boolean processCommand(String input) {

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

        utils.StringPair output;
        ReturnableCommand action = returnableCommands.get(commandName);
        if (action != null) {
            output = action.execute(args);
        } else {

            String executableFile = consoleState.findFileOnPath(commandName);
            if (executableFile != null) {
                output = utils.FileUtils.runExternalProgram(commandName, args);
            } else {
                utils.Printer.println(commandName + ": command not found");
                return false;
            }

        }

        context.ContextManager.outPutManager(output, redirectFrom, fileToRedirectTo, redirectionType);
        return true;

    }

    private utils.StringPair echo(ArrayList<String> args) {

        StringBuilder output = new StringBuilder();
        String error = "";

        for (String arg : args) {
            if (CommandUtils.REDIRECT_TOKENS.contains(arg)) break;
            output.append(arg).append(" ");
        }

        return new utils.StringPair(output.toString(), error);
    }

    private utils.StringPair pwd(ArrayList<String> args) {
        return new utils.StringPair(consoleState.getCWD(), "");
    }

    private utils.StringPair type(ArrayList<String> args) {
        final String commandName = args.get(0);

        if (CommandConstants.ALL_COMMANDS.contains(commandName)) {
            String output = String.format("%s is a shell builtin", commandName);
            return new utils.StringPair(output, "");
        }

        final String executableFile = consoleState.findFileOnPath(commandName);
        if (executableFile != null) {
            String output = String.format("%s is %s", commandName, executableFile);
            return new utils.StringPair(output, "");
        }

        String error = String.format("%s: not found", commandName);
        return new utils.StringPair("", error);
    }

    // NON_RETURNABLE COMMANDS

    private void cd(ArrayList<String> args) {

        String path = args.get(0);

        // Check if the user wants to go back
        if (path.startsWith("~")) {
            consoleState.setCurrentDir(System.getenv("HOME"));
            return;
        }

        if (consoleState.goBack(path)) return;

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

        utils.Printer.println("cd: " + path + ": No such file or directory");
    }

    private void exit(ArrayList<String> args) {
        scanner.close();
        System.exit(Integer.parseInt(args.get(0)));
    }

}
