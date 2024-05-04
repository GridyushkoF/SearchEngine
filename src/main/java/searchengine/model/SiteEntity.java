package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
public class SiteEntity {
    private static final String VARCHAR_TYPE = "VARCHAR(255)";
    @Id
    @TableGenerator(
            name = "siteEntityGen", // Уникальное имя генератора
            table = "siteIdGen", // Название таблицы для хранения идентификаторов
            pkColumnName = "siteKeyGen", // Название столбца с ключом генератора
            valueColumnName = "siteNextValGen", // Название столбца со значением следующего идентификатора
            pkColumnValue = "SiteEntity", // Значение, связанное с этим генератором
            allocationSize = 200 // Количество идентификаторов, выделяемых за один раз
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "siteEntityGen")
    private Integer id;
    @Enumerated(EnumType.STRING)
    private SiteStatus status;
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = VARCHAR_TYPE, nullable = false)
    private String url;
    @Column(columnDefinition = VARCHAR_TYPE, nullable = false)
    private String name;
    public SiteEntity(SiteStatus status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
}