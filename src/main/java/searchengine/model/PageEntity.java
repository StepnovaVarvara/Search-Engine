package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(name = "page")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity siteEntity;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, Index(path(512))") // TODO сделать нормально!
    private String pagePath;

    @NotNull
    @Column(name = "code")
    private int responseCode;

    @NotNull
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String contentPage;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "page")
    private List<IndexEntity> indexEntityList;
}
