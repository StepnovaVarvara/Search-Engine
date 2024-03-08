package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "statistic-settings")
public class StatisticsProperties {
    private Statuses statuses;
    private StatusMessages statusMessages;
}
