import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        commands.put(CommandConstants.CAT, this::cat);

        this.strPath = System.getenv("PATH");

        setCurrentDir(System.getProperty("user.dir"));

        Collections.addAll(this.paths, strPath.split(":"));
    }

    public boolean runCommand(Scanner scan, String input) {

        this.scanner = scan;

        String[] commandNameAndCleanedInput = getCommandAndCleanInput(input);
        String commandName = commandNameAndCleanedInput[0];
        String cleanedInput = commandNameAndCleanedInput[1];

        if (commandName.equals(CommandConstants.ECHO) && (cleanedInput.endsWith("'") || cleanedInput.endsWith("\""))) {
            echo(cleanedInput);
            return true;
        }

        ArrayList<String> args = quoterCleaner(cleanedInput);
        Command action = commands.get(commandName);

        // for (String arg : args) System.err.println("FILE_NAME: " + arg);

        if (action != null) {
            action.execute(args);
            return true;
        }

        // Check on environment variables
        String executableFile = this.findFileOnPath(commandName);
        if (executableFile != null) {
            runExternalProgram(commandName, args);
            return true;
        }

        System.out.println(commandName + ": command not found");
        return false;
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

    // private ArrayList<String> getArgs(String input) {

    //     int firstSpace = input.indexOf(" ");
    //     if (firstSpace == -1 || firstSpace == input.length() - 1) {
    //         return args;
    //     }

    //     String cleanInput = input.substring(firstSpace + 1).trim();

    // }

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

    private void runExternalProgram(String filePath, ArrayList<String> args) {

        try {
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add(filePath);
            fullCommand.addAll(args);
            Collections.addAll(fullCommand);

            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try(Scanner output = new Scanner(process.getInputStream())) {
                while (output.hasNextLine()) {
                    System.out.println(output.nextLine());
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.out.println("Failed to run program: " + e.getMessage());
        }
    }

    private void cat(ArrayList<String> input) {

        for (String fileName : input) {
            Path path = Path.of(fileName);
            try {
                String content = Files.readString(path);
                System.out.print(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void echo(String inputLine) {
        // 1. Parse "echo ...whatever..." into tokens (stripping quotes, etc.)
        ArrayList<String> tokens = quoterCleaner(inputLine);

        // 2. Join those tokens with exactly one space between them
        if (!tokens.isEmpty()) {
            System.out.println(String.join(" ", tokens));
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
