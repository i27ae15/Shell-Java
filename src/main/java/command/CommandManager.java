package command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import utils.StringPair;


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
        voidCommands.put(CommandConstants.HISTORY, this::history);

        this.consoleState = consoleState;
        this.scanner = scanner;
    }

    private boolean pipeLineManager(String input) {
        String[] commands = input.split("\\s*\\|\\s*");
        List<ProcessBuilder> processBuilders = new ArrayList<>();

        // First, identify the very last command in the pipeline.
        String lastCommandString = commands[commands.length - 1];
        String[] lastCommandParts = CommandUtils.getCommandAndCleanInput(lastCommandString);
        String lastCommandName = lastCommandParts[0];
        ReturnableCommand lastAction = returnableCommands.get(lastCommandName);

        // CASE 1: The pipeline ends with a built-in command.
        if (lastAction != null) {
            // Build a pipeline for all commands that come BEFORE the final built-in.
            for (int i = 0; i < commands.length - 1; i++) {
                String[] parts = CommandUtils.getCommandAndCleanInput(commands[i]);
                String name = parts[0];
                String cleaned = parts[1];
                ArrayList<String> args = CommandUtils.quoterCleaner(cleaned);
                String executableFile = consoleState.findFileOnPath(name);
                if (executableFile == null) {
                    utils.Printer.println(name + ": Command not found");
                    return true;
                }
                ArrayList<String> fullCommand = new ArrayList<>();
                fullCommand.add(executableFile);
                fullCommand.addAll(args);
                processBuilders.add(new ProcessBuilder(fullCommand));
            }

            try {
                // If there was a pipeline before the built-in (e.g., the 'ls' command)...
                if (!processBuilders.isEmpty()) {
                    List<Process> precedingProcesses = ProcessBuilder.startPipeline(processBuilders);
                    // We must wait for the preceding processes to finish.
                    for (Process p : precedingProcesses) {
                        p.waitFor();
                    }
                }

                // Now that the preceding pipeline is done, execute the final built-in command.
                ArrayList<String> lastArgs = CommandUtils.quoterCleaner(lastCommandParts[1]);
                StringPair output = lastAction.execute(lastArgs);
                context.ContextManager.outPutManager(output, null, null, null);

            } catch (java.io.IOException | InterruptedException e) {
                utils.Printer.println("Pipeline execution failed: " + e.getMessage());
            }

            return true; // We have fully handled this pipeline.

        } else {
            // CASE 2: The pipeline ends with an external command.
            // This logic is for pipelines like 'echo | wc'.
            for (String command : commands) {
                String[] parts = CommandUtils.getCommandAndCleanInput(command);
                String name = parts[0];
                ArrayList<String> args = CommandUtils.quoterCleaner(parts[1]);
                String executableFile = consoleState.findFileOnPath(name);
                if (executableFile == null) {
                    utils.Printer.println(name + ": Command not found");
                    return true;
                }
                ArrayList<String> fullCommand = new ArrayList<>();
                fullCommand.add(executableFile);
                fullCommand.addAll(args);
                processBuilders.add(new ProcessBuilder(fullCommand));
            }

            try {
                List<Process> processes = ProcessBuilder.startPipeline(processBuilders);
                Process lastProcess = processes.get(processes.size() - 1);
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(lastProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        utils.Printer.println(line);
                    }
                }
                for (Process process : processes) {
                    process.waitFor();
                }
            } catch (java.io.IOException | InterruptedException e) {
                utils.Printer.println("Pipeline execution failed: " + e.getMessage());
            }
            return true;
        }
    }

    public boolean processCommand(String input) {

        consoleState.addToHistory(input);
        if (input.contains("|")) return pipeLineManager(input);

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

    private void history(ArrayList<String> args) {
        consoleState.printHistory();
    }

}
