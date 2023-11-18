package searchengine.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

@Entity
@Table(name = "search_index")
@Data
@NoArgsConstructor


public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"),name = "page_id",nullable = false)
    private Page page;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"),name = "lemma_id",nullable = false)
    private Lemma lemma;
    @Column
    private float ranking;

    public SearchIndex(Page page, Lemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.ranking = rank;
    }
}
