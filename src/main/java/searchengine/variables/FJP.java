package searchengine.variables;

import lombok.Data;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;

@Data
@Slf4j
public class FJP {
    private static ForkJoinPool INSTANCE;

//    @Value("${thread.count}")
    private static int threadCount = 6;

    public static ForkJoinPool getInstance() {
        log.info("Кол-во тредов > {}", threadCount);
        if (INSTANCE == null || INSTANCE.isShutdown()) {
            INSTANCE = new ForkJoinPool(threadCount);
        }
        return INSTANCE;
    }
}