package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private StatusType statusIndexing;

    @NotNull
    @Column(name = "status_time", columnDefinition = "DATETIME")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String textOfLastError;

    @NotNull
    @Column(name = "url", columnDefinition = "VARCHAR(255)")
    private String siteUrl;

    @NotNull
    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    private String siteName;
}
