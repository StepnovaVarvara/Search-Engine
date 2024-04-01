package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(name = "lemma")
@Getter
@Setter
@Accessors(chain = true)
public class LemmaEntity implements Comparable<LemmaEntity> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)", name = "lemma")
    private String lemmaName;

    @NotNull
    private int frequency;

    @OneToMany(mappedBy = "lemma", orphanRemoval = true)
    private List<IndexEntity> indexEntityList;

    @Override
    public int compareTo(LemmaEntity o) {
        return this.getFrequency() - o.getFrequency();
    }
}
