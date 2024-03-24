package searchengine.variables;

import lombok.Getter;

public class IndexProcessVariables {
    @Getter
    private static boolean RUNNING;
    public static void setRUNNING(boolean RUNNING) {
        IndexProcessVariables.RUNNING = RUNNING;
    }
}
