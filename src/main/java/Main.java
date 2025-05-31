import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine();

            if (input.equals(input)) {
                exit();
                break;
            }

            System.out.println(input + ": command not found");
        }

        scanner.close();

    }

    public static void exit() {
        System.out.println("exit 0");
    }
}
