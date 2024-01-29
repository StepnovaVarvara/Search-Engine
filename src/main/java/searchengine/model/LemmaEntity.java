package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "lemma")
@Getter
@Setter
@Accessors(chain = true)
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemmaName;

    @Column(name = "frequency")
    @NotNull
    private int countOfWordsPerPage;
}
