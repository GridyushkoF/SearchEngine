package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
public class Site {
    private static final String VARCHAR_TYPE = "VARCHAR(255)";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    public Site(SiteStatus status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
}