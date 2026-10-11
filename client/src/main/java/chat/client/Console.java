package chat.client;

public final class Console {

    private static volatile String prompt = "> ";
    private static volatile boolean promptVisible = false;

    private Console() {
    }

    public static synchronized void event(String text) {
        if (promptVisible) {
            System.out.print("\r");
        }
        System.out.println(text);
        if (promptVisible) {
            System.out.print(prompt);
        }
        System.out.flush();
    }

    public static synchronized void println(String text) {
        System.out.println(text);
        System.out.flush();
    }

    public static synchronized void prompt() {
        System.out.print(prompt);
        System.out.flush();
        promptVisible = true;
    }

    public static synchronized void setPrompt(String newPrompt) {
        prompt = newPrompt;
    }

    public static synchronized void hidePrompt() {
        promptVisible = false;
    }
}
