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
    @GeneratedValue
    private int id;
    @ManyToOne
    private Page page;
    @ManyToOne
    private Lemma lemma;
    private float rank;

}
