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

        String[] tokens = input.trim().split("\s+");
        String commandName = tokens[0].toLowerCase();


        if (commandName.equals(CommandConstants.ECHO) && (input.endsWith("'") || input.endsWith("\""))) {
            echo(input);
            return true;
        }

        ArrayList<String> args = getArgs(input);
        Command action = commands.get(commandName);

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

    private ArrayList<String> getArgs(String input) {
        ArrayList<String> args = new ArrayList<>();

        int firstSpace = input.indexOf(" ");
        if (firstSpace == -1 || firstSpace == input.length() - 1) {
            return args;
        }

        String cleanInput = input.substring(firstSpace + 1).trim();
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean escaping = false;

        for (int i = 0; i < cleanInput.length(); i++) {
            char c = cleanInput.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
            }
            else if (c == '\\' && !inSingleQuotes && !inDoubleQuotes) {
                escaping = true;
            }
            else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            }
            else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
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

        args.addAll(tokens);
        return args;
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
        ArrayList<String> tokens = getArgs(inputLine);

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
