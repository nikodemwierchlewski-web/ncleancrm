package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayDistrictRepository extends JpaRepository<DayDistrictEntity, LocalDate> {
    @EntityGraph(attributePaths = "district")
    List<DayDistrictEntity> findByDayGreaterThanEqualAndDayLessThan(LocalDate start, LocalDate end);
}
