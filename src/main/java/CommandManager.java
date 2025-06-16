import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private Scanner scanner;
    private final String strPath;
    private final ArrayList<String> paths = new ArrayList<>();
    private String cwd;

    public CommandManager() {
        commands.put(CommandConstants.ECHO, this::echo);
        commands.put(CommandConstants.TYPE, this::type);
        commands.put(CommandConstants.EXIT, this::exit);
        commands.put(CommandConstants.PWD, this::pwd);
        commands.put(CommandConstants.CD, this::cd);

        this.strPath = System.getenv("PATH");

        setCurrentDir(System.getProperty("user.dir"));

        Collections.addAll(this.paths, strPath.split(":"));
    }

    public boolean runCommand(Scanner scan, String input) {

        this.scanner = scan;

        String[] commandNameAndCleanedInput = getCommandAndCleanInput(input);
        String commandName = commandNameAndCleanedInput[0];
        String cleanedInput = commandNameAndCleanedInput[1];

        ArrayList<String> args = quoterCleaner(cleanedInput);
        Command action = commands.get(commandName);

        String[] filesToRedirect = getRedirection(args);

        String redirectFrom = filesToRedirect[0];
        String fileToRedirectTo = filesToRedirect[1];
        boolean isError = filesToRedirect[2] != null;

        if (commandName.equals(CommandConstants.ECHO)) {
            echo(args, redirectFrom, fileToRedirectTo, isError);
            return true;
        }

        if (commandName.equals(CommandConstants.CAT)) {
            cat(args, fileToRedirectTo, isError);
            return true;
        }

        if (action != null) {
            action.execute(args);
            return true;
        }

        // Check on environment path
        String executableFile = this.findFileOnPath(commandName);
        if (executableFile != null) {
            runExternalProgram(commandName, args, redirectFrom, fileToRedirectTo);
            return true;
        }

        System.out.println(commandName + ": command not found");
        return false;
    }

    private String[] getRedirection(ArrayList<String> args) {
        String[] toReturn = new String[3];

        for (int i = 0; i < args.size(); i++) {
            String c = args.get(i);
            if (c.equals("1>") || c.equals(">") || c.equals("2>")) {

                toReturn[0] =  args.get(i - 1);
                toReturn[1] =  args.get(i + 1);

                if (c.equals("2>")) toReturn[2] = "e";

                break;

            }
        }
        return toReturn;
    }

    private String[] getCommandAndCleanInput(String input) {
        String[] result = new String[2];

        String commandName = "";
        String cleanedInput = "";

        if (input.charAt(0) != '\'' && input.charAt(0) != '"') {
            String[] tokens = input.trim().split("\s+");

            commandName = tokens[0].toLowerCase();
            cleanedInput = input.substring(commandName.length());
        }
        else {

            // get first character
            char firstChar = input.charAt(0);

            int firstIndex = input.indexOf(firstChar);
            int secondIndex = input.indexOf(firstChar, firstIndex + 1);

            // Clean the command
            commandName = input.substring(firstIndex, secondIndex + 1);
            commandName = quoterCleaner(commandName).get(0);

            cleanedInput = input.substring(secondIndex + 1).replaceFirst("^\\s+", "");

        }

        result[0] = commandName;
        result[1] = cleanedInput;

        return result;

    }

    private ArrayList<String> quoterCleaner(String input) {
        ArrayList<String> result = new ArrayList<>();

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean escaping = false;

        int parentQuote = -1;  // -1 for no parent, 0 for ' & 1 for "

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {

                if (inDoubleQuotes) {
                    switch (c) {
                        case '\\', '"', '$', '\n':
                            current.append(c);
                            break;
                        default:
                            current.append('\\');
                            current.append(c);
                            break;
                    }
                } else {
                    current.append(c);
                }
                escaping = false;
            }
            else if (c == '\\' && !inSingleQuotes) {
                escaping = true;
            }
            else if (c == '"') {

                if (!inSingleQuotes && parentQuote == -1) {
                    parentQuote = 1;
                    inDoubleQuotes = true;
                }
                else if (inSingleQuotes && parentQuote == 0) {
                    current.append('"');
                    inDoubleQuotes = !inDoubleQuotes;
                }
                else if (inDoubleQuotes && parentQuote == 1) {
                    inDoubleQuotes = false;
                    inSingleQuotes = false;
                    parentQuote = -1;
                }

            }
            else if (c == '\'') {

                if (!inDoubleQuotes && parentQuote == -1) {
                    parentQuote = 0;
                    inSingleQuotes = true;
                }
                else if (inDoubleQuotes && parentQuote == 1) {
                    current.append('\'');
                    inSingleQuotes = !inSingleQuotes;
                }
                else if (inSingleQuotes && parentQuote == 0) {
                    inSingleQuotes = false;
                    inDoubleQuotes = false;
                    parentQuote = -1;
                }

            }
            else if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            }
            else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        result.addAll(tokens);
        return result;

    }

    private void runExternalProgram(
        String filePath,
        ArrayList<String> args,
        String fileToRedirectFrom,
        String fileToRedirectTo
    ) {

        boolean printOutput = (fileToRedirectFrom == null || fileToRedirectFrom.isEmpty());

        try {
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add(filePath);

            if (!printOutput) fullCommand.add(fileToRedirectFrom);
            if (printOutput) fullCommand.addAll(args);

            Collections.addAll(fullCommand);

            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder outputBuffer = new StringBuilder();

            try(Scanner output = new Scanner(process.getInputStream())) {
                while (output.hasNextLine()) {

                    String line = output.nextLine();

                    if (printOutput) {
                        System.out.println(line);
                    } else {
                        outputBuffer.append(line).append(System.lineSeparator());
                    }
                }
            }

            process.waitFor();

            if (!printOutput) FileWriter.writeToFile(outputBuffer.toString(), fileToRedirectTo);

        } catch (Exception e) {
            System.out.println("Failed to run program: " + e.getMessage());
        }

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
                    FileWriter.writeToFile(output, fileToRedirectTo);
                } catch (Exception e) {
                    System.out.println("Failed to write: " + e.getMessage());
                }
            } else {
                System.out.print(output);
            }
        }
    }

    private void echo(ArrayList<String> args, String redirectFrom, String fileToRedirectTo, boolean isError) {


        if (fileToRedirectTo != null && !fileToRedirectTo.isEmpty()) {
            try {
                if (isError) {
                    FileWriter.writeToFile("", fileToRedirectTo);
                    System.out.println(redirectFrom);
                } else {
                    FileWriter.writeToFile(redirectFrom + "\n", fileToRedirectTo);
                }
            } catch (Exception e) {
                System.out.println("Failed to run program: " + e.getMessage());
            }
            return;
        }

        if (!args.isEmpty()) {
            System.out.println(String.join(" ", args));
        }
    }


    private void echo(ArrayList<String> args) {
        System.out.println(String.join(" ", args));
    }

    private void pwd(ArrayList<String> args) {
        System.out.println(cwd);
    }

    private boolean goingBack(String path) {
        int count = 0;
        int index = 0;

        while ((index = path.indexOf("..", index)) != -1) {
            count++;
            index += 2;
        }

        if (count > 0) {

            // User wants to go back
            // Delete the N last folders
            String[] elements = cwd.split("/");

            int toAdd = elements.length - count;

            if (0 >= toAdd) {
                System.out.println("cd: " + path + ": No such file or directory");
                return false;
            }

            cwd = "";
            for (int i = 0; toAdd > i; i++) {

                if (toAdd - i == 1) {
                    cwd += elements[i];
                }
                else {
                    cwd += elements[i] + "/";
                }

            }

            return true;
        }

        return false;
    }

    private void cd(ArrayList<String> args) {

        String path = args.get(0);

        // Check if the user wants to go back
        if (path.startsWith("~")) {
            setCurrentDir(System.getenv("HOME"));
            return;
        }

        if (path.startsWith("~")) {
            setCurrentDir(System.getenv("HOME"));
            return;
        }
        if (goingBack(path)) return;
        if (path.startsWith("~")) {
            setCurrentDir(System.getenv("HOME"));
            return;
        }


        if (path.startsWith("./")) {
            // delete the .
            path = path.substring(1, path.length());

            // Add the cwd at front
            path = cwd + path;
        }

        // Check for absolute path
        File dir = new File(path);

        if (dir.exists() && dir.isDirectory()) {
            setCurrentDir(path);
            return;
        }

        System.out.println("cd: " + path + ": No such file or directory");
    }

    private void type(ArrayList<String> args) {
        String commandName = args.get(0);

        // Look on the path first:
        if (CommandConstants.ALL_COMMANDS.contains(commandName)) {
            System.out.println(commandName + " is a shell builtin");
            return;
        }

        String executableFile = this.findFileOnPath(commandName);
        if (executableFile != null) {
            System.out.println(commandName + " is " + executableFile);
            return;
        }

        System.out.println(commandName + ": not found");

    }

    private void exit(ArrayList<String> args) {
        scanner.close();
        System.exit(Integer.parseInt(args.get(0)));
    }


    // ----------------------------------------------------------
    // Utils

    private String findFileOnPath(String fileName) {
        // Check each dir in PATH for executable

        for (String dir : paths) {
            File file = new File(dir, fileName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private void setCurrentDir(String absolutePath) {
        if (absolutePath.endsWith("/")) {
            absolutePath = absolutePath.substring(0, absolutePath.length() - 1);
        }

        cwd = absolutePath;
    }

}
