package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lemma",indexes = {@Index(name = "lemma_string_FK",columnList = "lemma")})
@Data
@NoArgsConstructor

public class LemmaEntity {
    @Id
    @TableGenerator(
            name = "lemmaEntityGen", // Уникальное имя генератора
            table = "lemmaIdGen", // Название таблицы для хранения идентификаторов
            pkColumnName = "lemmaKeyGen", // Название столбца с ключом генератора
            valueColumnName = "lemmaNextValGen", // Название столбца со значением следующего идентификатора
            pkColumnValue = "LemmaEntity", // Значение, связанное с этим генератором
            allocationSize = 200 // Количество идентификаторов, выделяемых за один раз
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "lemmaEntityGen")
    private Integer id;
    @ManyToOne(cascade = CascadeType.REFRESH,fetch = FetchType.LAZY)
    private SiteEntity site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    private int frequency;
    public LemmaEntity(SiteEntity Site, String lemma, int frequency) {
        this.site = Site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
    @Override
    public String toString() {
        return lemma;
    }
}
