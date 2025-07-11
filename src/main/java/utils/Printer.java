package utils;

public class Printer {

    public static void print(String msg){
        System.out.print("\r"+msg);
    }

    public static void println(String msg){
        System.out.println("\r" + msg);
    }

    public static void printPrompt(String msg) {
        // 1. Use print(), not println().
        System.out.print(msg);

        // 2. Force the buffered output to be written immediately.
        System.out.flush();
    }

}
