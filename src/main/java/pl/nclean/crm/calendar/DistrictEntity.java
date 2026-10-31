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
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
