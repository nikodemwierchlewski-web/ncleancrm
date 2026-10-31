package pl.nclean.crm.calendar;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "day_district")
public class DayDistrictEntity {
    @EmbeddedId
    private DayDistrictId id;
    @MapsId("districtId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private DistrictEntity district;

    protected DayDistrictEntity() {}
    DayDistrictEntity(LocalDate day, DistrictEntity district) {
        this.id = new DayDistrictId(day, district.getId());
        this.district = district;
    }
    public DayDistrictId getId() { return id; }
    public LocalDate getDay() { return id.getDay(); }
    public DistrictEntity getDistrict() { return district; }
}
