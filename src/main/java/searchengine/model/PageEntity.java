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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @ManyToOne(cascade = CascadeType.REFRESH,fetch = FetchType.EAGER)
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
