import java.io.File;
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

        String[] tokens = input.trim().split("\\s+");
        String commandName = tokens[0].toLowerCase();

        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);

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

    private void runExternalProgram(String filePath, String[] args) {

        try {
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add(filePath);
            Collections.addAll(fullCommand, args);

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

    private void echo(String[] args) {
        System.out.println(String.join(" ", args));
    }

    private void pwd(String[] args) {
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

    private void cd(String[] args) {

        String path = args[0];

        // Check if the user wants to go back

        if (goingBack(path)) return;


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

    private void type(String[] args) {
        String commandName = args[0];

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

    private void exit(String[] args) {
        scanner.close();
        System.exit(Integer.parseInt(args[0]));
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
