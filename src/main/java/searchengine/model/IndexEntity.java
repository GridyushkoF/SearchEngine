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
    @TableGenerator(
            name = "indexEntityGen", // Уникальное имя генератора
            table = "indexIdGen", // Название таблицы для хранения идентификаторов
            pkColumnName = "indexKeyGen", // Название столбца с ключом генератора
            valueColumnName = "indexNextValGen", // Название столбца со значением следующего идентификатора
            pkColumnValue = "IndexEntity", // Значение, связанное с этим генератором
            allocationSize = 200 // Количество идентификаторов, выделяемых за один раз
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "indexEntityGen")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.REFRESH)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_page_id"), name = "page_id", nullable = false)
    private PageEntity page;
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.REFRESH)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_index_lemma_id"), name = "lemma_id", nullable = false)
    private LemmaEntity lemmaEntity;
    @Column
    private int ranking;

    public IndexEntity(PageEntity page, LemmaEntity lemmaEntity, int rank) {
        this.page = page;
        this.lemmaEntity = lemmaEntity;
        this.ranking = rank;
    }

}
