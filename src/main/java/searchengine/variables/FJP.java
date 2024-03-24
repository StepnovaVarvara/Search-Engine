package searchengine.variables;

import lombok.Data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ForkJoinPool;

@Data
@Slf4j
public final class FJP {
    private static ForkJoinPool INSTANCE;

//    @Value("${thread.count}")
    private static int threadCount = 6; //TODO не получается присвоить переменной
                                        // значение из конфигурационного файла > выдает 0
    public static ForkJoinPool getInstance() {
        log.info("Кол-во тредов > {}", threadCount);
        if (INSTANCE == null || INSTANCE.isShutdown()) {
            INSTANCE = new ForkJoinPool(threadCount);
        }
        return INSTANCE;
    }
}