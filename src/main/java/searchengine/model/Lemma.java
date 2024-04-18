package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lemma",indexes = {@Index(name = "lemma_string_FK",columnList = "lemma")})
@Data
@NoArgsConstructor

public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.REFRESH)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    private int frequency;
    public Lemma(Site Site, String lemma, int frequency) {
        this.site = Site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
    @Override
    public String toString() {
        return lemma;
    }
}
