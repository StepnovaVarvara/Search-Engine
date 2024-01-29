package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
//@Component
//@ConfigurationProperties(prefix = "thread-count")
public class ThreadCount {
    private int countOfThread;
}
