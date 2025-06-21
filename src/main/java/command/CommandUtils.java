package command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandUtils {

    public static final Set<String> REDIRECT_TOKENS = Set.of(">", "1>", "2>", ">>", "1>>", "2>>");

    static ArrayList<String> quoterCleaner(String input) {
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

    static String[] getRedirection(ArrayList<String> args) {
        String[] toReturn = new String[3];

        for (int i = 0; i < args.size(); i++) {
            String c = args.get(i);
            if (REDIRECT_TOKENS.contains(c)) {

                toReturn[0] = args.get(i - 1);
                toReturn[1] = args.get(i + 1);
                toReturn[2] = c;
                break;

            }
        }
        return toReturn;
    }

    static String[] getCommandAndCleanInput(String input) {
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

}
