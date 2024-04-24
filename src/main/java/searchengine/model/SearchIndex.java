package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_index",indexes = {@Index(name = "lemma_id_FK",columnList = "lemma_id"),@Index(name = "page_id_FK",columnList = "page_id")})
@Data
@NoArgsConstructor


public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private int id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"), name = "page_id", nullable = false)
    private Page page;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"), name = "lemma_id", nullable = false)
    private Lemma lemma;
    @Column
    private int ranking;

    public SearchIndex(Page Page, Lemma Lemma, int rank) {
        this.page = Page;
        this.lemma = Lemma;
        this.ranking = rank;
    }

}
