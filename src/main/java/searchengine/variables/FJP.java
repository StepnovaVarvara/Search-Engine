package searchengine.variables;

import lombok.Data;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ForkJoinPool;

@Data
@Slf4j
@Component
public class FJP {
    private static ForkJoinPool INSTANCE;

    public static ForkJoinPool getInstance(Integer count) {
        log.info("Кол-во тредов > {}", count);
        if (INSTANCE == null || INSTANCE.isShutdown()) {
            INSTANCE = new ForkJoinPool(count);
        }
        return INSTANCE;
    }
}