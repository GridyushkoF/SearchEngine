package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_index", indexes = {@Index(name = "lemma_id_FK", columnList = "lemma_id"), @Index(name = "page_id_FK", columnList = "page_id")})
@Data
@NoArgsConstructor


public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.REFRESH)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"), name = "page_id", nullable = false)
    private PageEntity page;
    @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.REFRESH)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"), name = "lemma_id", nullable = false)
    private LemmaEntity lemmaEntity;
    @Column
    private int ranking;

    public IndexEntity(PageEntity page, LemmaEntity LemmaEntity, int rank) {
        this.page = page;
        this.lemmaEntity = LemmaEntity;
        this.ranking = rank;
    }

}
