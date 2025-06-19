public class ContextManager {

    private static final String REDIRECTION = ">";
    private static final String REDIRECTION1 = "1>";
    private static final String ERROR_REDIRECTION = "2>";
    private static final String APPEND_REDIRECTION = ">>";
    private static final String APPEND_REDIRECTION1 = "1>>";
    private static final String APPEND_ERROR = "2>>";

    public static void outPutManager(
        StringPair commandResult,
        String redirectFrom,
        String redirectTo,
        String redirectionType
    ) {

        String output = commandResult.first();
        String error = commandResult.second();

        if (redirectTo == null) {

            if (!output.isEmpty()) {
                ContextManager.print(output);
            } else if (!error.isEmpty()) {
                ContextManager.print(error);
            }

            return;
        }
        else if (redirectTo != null) {

            switch (redirectionType) {
                case ContextManager.REDIRECTION, ContextManager.REDIRECTION1:
                    FileUtils.writeToFile(output, redirectTo);
                    if (!error.isEmpty()) ContextManager.print(error);
                    break;

                case ContextManager.APPEND_REDIRECTION, ContextManager.APPEND_REDIRECTION1:
                    if (FileUtils.fileExists(redirectTo)) {
                        FileUtils.appendToFile('\n' + output, redirectTo);
                    } else {
                        FileUtils.writeToFile(output, redirectTo);
                    }
                    if (!error.isEmpty()) ContextManager.print(error);
                    break;

                case ContextManager.ERROR_REDIRECTION:
                    FileUtils.writeToFile(error, redirectTo);
                    if (!output.isEmpty()) ContextManager.print(output);
                    break;

                case ContextManager.APPEND_ERROR:

                    if (FileUtils.fileExists(redirectTo)) {
                        FileUtils.appendToFile(error, redirectTo);
                    } else {
                        FileUtils.writeToFile(error, redirectTo);
                    }
                    if (!output.isEmpty()) ContextManager.print(output);
                    break;

                default:
                    break;
            }

        }

    }

    private static void print(String toPrint) {
        System.out.print(toPrint);
        if (!toPrint.isEmpty() && toPrint.charAt(toPrint.length() - 1) != '\n') {
            System.out.println();
        }
    }

}
