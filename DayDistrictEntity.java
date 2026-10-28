package pl.nclean.crm.calendar;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "day_district")
public class DayDistrictEntity {
    @Id
    private LocalDate day;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private DistrictEntity district;

    protected DayDistrictEntity() {}
    DayDistrictEntity(LocalDate day, DistrictEntity district) {
        this.day = day;
        this.district = district;
    }
    public LocalDate getDay() { return day; }
    public DistrictEntity getDistrict() { return district; }
}
