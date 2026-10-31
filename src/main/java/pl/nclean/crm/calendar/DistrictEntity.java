package pl.nclean.crm.calendar;

import jakarta.persistence.*;

@Entity
@Table(name = "district")
public class DistrictEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false, length = 7)
    private String color;
    @Column(nullable = false)
    private boolean active;

    protected DistrictEntity() {}
    DistrictEntity(String name, String color) { this.name = name; this.color = color; this.active = true; }
    void update(String name, String color) { this.name = name; this.color = color; }
    void setActive(boolean active) { this.active = active; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isActive() { return active; }
}
