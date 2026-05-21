package hoangtlt.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tables")
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    public enum TableStatus {
        AVAILABLE, OCCUPIED, RESERVED
    }

    public DiningTable() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
}
