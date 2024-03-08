package searchengine.variables;

import java.util.concurrent.ForkJoinPool;

public class FJP {
    private static ForkJoinPool INSTANCE;

    //TODO 10 get from application yalm

    public static ForkJoinPool getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ForkJoinPool(50);
        }
        return INSTANCE;
    }
}