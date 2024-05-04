package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "page", indexes = @Index(columnList = "path"))
@Data
@NoArgsConstructor
public class PageEntity {
    @Id
    @TableGenerator(
            name = "pageEntityGen", // Уникальное имя генератора
            table = "pageIdGen", // Название таблицы для хранения идентификаторов
            pkColumnName = "pageKeyGen", // Название столбца с ключом генератора
            valueColumnName = "pageNextValGen", // Название столбца со значением следующего идентификатора
            pkColumnValue = "PageEntity", // Значение, связанное с этим генератором
            allocationSize = 200 // Количество идентификаторов, выделяемых за один раз
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "pageEntityGen")
    private Integer id;
    @ManyToOne(cascade = CascadeType.REFRESH,fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private SiteEntity site;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @Enumerated(EnumType.STRING)
    private PageStatus pageStatus;
    public PageEntity(SiteEntity Site, String path, int code, String content, PageStatus pageStatus) {
        this.site = Site;
        this.path = path;
        this.code = code;
        this.content = content;
        this.pageStatus = pageStatus;
    }
    @Override
    public String toString() {
        return "id: " + id + " path: " + path;
    }
}
