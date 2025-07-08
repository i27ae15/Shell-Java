package context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConsoleState {

    private final String strPath;
    private final ArrayList<String> paths = new ArrayList<>();
    private String cwd;
    private autocompletion.Trie autocompletionTrie;
    private utils.StringPair lastAutoCompletionCalled;
    private ArrayList<String> lastAutocompletionOptions;
    private ArrayList<String> history;
    private int historyIdx;

    public ConsoleState() {
        this.strPath = System.getenv("PATH");

        setCurrentDir(System.getProperty("user.dir"));

        Collections.addAll(this.paths, strPath.split(":"));

        autocompletionTrie = new autocompletion.Trie();
        loadFilesToTrie();

        lastAutoCompletionCalled = new utils.StringPair(cwd, "no-last-input");
        lastAutocompletionOptions = new ArrayList<>();
        history = new ArrayList<>();
        historyIdx = 0;

    }

    private void loadFilesToTrie() {

        for (String dir : paths) {

            File directory = new File(dir);

            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                if (files == null) continue;

                for (File file : files) {
                    autocompletionTrie.addWord(file.getName());
                }

            }

        }

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

    private String multipleAutocompletionManager(String current, ArrayList<String> options) {
        Set<Integer> sizes = new HashSet<>();

        String nextOption = "";
        int minSize = current.length();

        for (String option : options) {

            if (sizes.contains(option.length())) {
                return "";
            }

            if ((nextOption.length() > option.length() || nextOption.isEmpty()) && option.length() > minSize) {
                nextOption = option;
            }

            sizes.add(option.length());
        }

        return nextOption;

    }

    public String autocompletionManager(String input) {
        String workingDir = lastAutoCompletionCalled.first();
        String toComplete = lastAutoCompletionCalled.second();

        if (workingDir.equals(cwd) && toComplete.equals(input)) {
            StringBuilder toPrint = new StringBuilder();

            for (String opt : lastAutocompletionOptions) {
                toPrint.append(opt + "  ");
            }
            utils.Printer.println("$ " + toComplete);
            utils.Printer.println(toPrint.toString());
            utils.Printer.println("$ " + toComplete);
            return "";
        }

        lastAutocompletionOptions = autocompletionTrie.getPossibleOptions(input);
        lastAutoCompletionCalled = new utils.StringPair(workingDir, input);

        if (lastAutocompletionOptions.size() > 1 || lastAutocompletionOptions.size() == 0) {

            String nextOption = multipleAutocompletionManager(input, lastAutocompletionOptions);

            if (nextOption.isEmpty()) {
                utils.Printer.print(String.valueOf('\u0007'));
                return "";
            }

            return nextOption;
        }

        String onlyOption = lastAutocompletionOptions.get(0);
        return onlyOption + " ";
    }

    public void addToHistory(String toHistory) {
        history.add(toHistory);
        historyIdx = history.size() - 1;
    }

    public void printHistory(int limit) {
        int i = 0;

        if (limit != -1) i = history.size() - limit;

        for (; history.size() > i; i++) {
            utils.Printer.println("    " + String.valueOf(i + 1) + "  " + history.get(i));
        }

    }

    public String getPreviousCommand() {

        String command = history.get(historyIdx);

        historyIdx = historyIdx > 0 ? historyIdx - 1 : 0;

        return command;
    }

    public String getNextCommand() {
        String command = history.get(historyIdx);

        historyIdx = history.size() - 1 > historyIdx ? historyIdx + 1 : history.size();

        return command;
    }

}
