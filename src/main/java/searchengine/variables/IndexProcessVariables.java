package searchengine.variables;

import lombok.Getter;

public class IndexProcessVariables {
    @Getter
    private static boolean RUNNING;

    public static void setRUNNING(boolean RUNNING) {
        IndexProcessVariables.RUNNING = RUNNING;
    }

    public static final String STOP_MESSAGE = "Индексация остановлена пользователем";
}
