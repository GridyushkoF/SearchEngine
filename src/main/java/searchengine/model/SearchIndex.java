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
    @ManyToOne(cascade = CascadeType.REFRESH)
    private Page page;
    @ManyToOne(cascade = CascadeType.REFRESH)
    private Lemma lemma;
    @Column
    private float ranking;

    public SearchIndex(Page page, Lemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.ranking = rank;
    }
}
