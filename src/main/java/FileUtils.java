import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileUtils {

    static StringPair runExternalProgram(
        String filePath,
        ArrayList<String> args
    ) {

        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add(filePath);

            for (String arg : args) {
                if (CommandUtils.REDIRECT_TOKENS.contains(arg)) break;
                fullCommand.add(arg);
            }

            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try(Scanner fileOutput = new Scanner(process.getInputStream())) {
                while (fileOutput.hasNextLine()) {

                    String line = fileOutput.nextLine();

                    if (line.contains("No such file or directory")) {
                        error.append(line + '\n');
                    } else {
                        output.append(line + '\n');
                    }

                }
            }

            process.waitFor();

        } catch (Exception e) {
            error.append("Failed to run program: " + e.getMessage());
        }

        return new StringPair(output.toString(), error.toString());
    }

    public static boolean appendToFile(String content, String filePath) {
        try {
            Files.writeString(
                Path.of(filePath),
                content,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            );

        } catch (IOException e) {
            System.out.println("There was an error while writing to file: " + filePath);
            return false;
        }
        return true;
    }

    public static boolean writeToFile(String content, String filePath) {
        try {
            Files.writeString(
                Path.of(filePath),
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            System.out.println("There was an error while opening the file: " + filePath);
            return false;
        }
        return true;

    }

    public static boolean appendToFileOrCreate(String content, String filePath) {
        Path path = Path.of(filePath);
        if (Files.exists(path)) {
            return appendToFile(content, filePath);
        } else {
            return writeToFile(content, filePath);
        }
    }

    public static boolean fileExists(String filePath) {
        return Files.exists(Path.of(filePath));
    }

}
