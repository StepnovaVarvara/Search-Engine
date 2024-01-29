package searchengine.variables;

import lombok.Getter;

import java.util.concurrent.ForkJoinPool;

public class IndexProcessVariables {
    @Getter
    private static boolean RUNNING;

    public static void setRUNNING(boolean RUNNING) {
        IndexProcessVariables.RUNNING = RUNNING;
    }

    public static final String STOP_MESSAGE = "Индексация остановлена пользователем";

    //public static ForkJoinPool forkJoinPoolPage = new ForkJoinPool(1000);
}
