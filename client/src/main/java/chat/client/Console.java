package chat.client;

/**
 * Salida de consola compartida entre el hilo que lee comandos del usuario y
 * los hilos de Ice que entregan callbacks. Todas las escrituras estan
 * sincronizadas para que los mensajes asincronos no se mezclen entre si y
 * el prompt se vuelva a dibujar despues de cada evento.
 */
public final class Console {

    private static final String PROMPT = "> ";
    private static volatile boolean promptVisible = false;

    private Console() {
    }

    /** Imprime un evento asincrono sin romper la linea del prompt. */
    public static synchronized void event(String text) {
        if (promptVisible) {
            System.out.print("\r");
        }
        System.out.println(text);
        if (promptVisible) {
            System.out.print(PROMPT);
        }
        System.out.flush();
    }

    /** Imprime una linea normal (respuesta directa a un comando). */
    public static synchronized void println(String text) {
        System.out.println(text);
        System.out.flush();
    }

    public static synchronized void prompt() {
        System.out.print(PROMPT);
        System.out.flush();
        promptVisible = true;
    }

    public static synchronized void hidePrompt() {
        promptVisible = false;
    }
}
