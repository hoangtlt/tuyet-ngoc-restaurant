package hoangtlt.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "areas")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "area", cascade = CascadeType.ALL)
    private List<DiningTable> tables;

    public Area() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<DiningTable> getTables() { return tables; }
    public void setTables(List<DiningTable> tables) { this.tables = tables; }
}
