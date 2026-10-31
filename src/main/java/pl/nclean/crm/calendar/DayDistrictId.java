package pl.nclean.crm.calendar;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DayDistrictId implements Serializable {
    @Column(name = "day", nullable = false)
    private LocalDate day;
    @Column(name = "district_id", nullable = false)
    private Long districtId;

    protected DayDistrictId() {}
    DayDistrictId(LocalDate day, Long districtId) { this.day = day; this.districtId = districtId; }
    public LocalDate getDay() { return day; }
    public Long getDistrictId() { return districtId; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof DayDistrictId id
            && Objects.equals(day, id.day) && Objects.equals(districtId, id.districtId);
    }
    @Override public int hashCode() { return Objects.hash(day, districtId); }
}
