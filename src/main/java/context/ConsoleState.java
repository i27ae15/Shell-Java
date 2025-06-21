package context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ConsoleState {

    private final String strPath;
    private final ArrayList<String> paths = new ArrayList<>();
    private String cwd;
    private autocompletion.Trie autocompletionTrie;

    public ConsoleState() {
        this.strPath = System.getenv("PATH");

        setCurrentDir(System.getProperty("user.dir"));

        Collections.addAll(this.paths, strPath.split(":"));
        autocompletionTrie = new autocompletion.Trie();

    }

    public String getCWD() { return cwd; }

    public boolean goBack(String path) {
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
                utils.Printer.println("cd: " + path + ": No such file or directory");
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

    public String findFileOnPath(String fileName) {
        // Check each dir in PATH for executable

        for (String dir : paths) {
            File file = new File(dir, fileName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public void setCurrentDir(String absolutePath) {
        if (absolutePath.endsWith("/")) {
            absolutePath = absolutePath.substring(0, absolutePath.length() - 1);
        }

        cwd = absolutePath;
    }

    public void printAutocompletion(String input) {

        ArrayList<String> options = autocompletionTrie.getPossibleOptions(input);

        for (String opt : options) {
            utils.Printer.print("$ " + opt + " ");
        }

    }

}
