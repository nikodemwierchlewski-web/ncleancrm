package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DayDistrictRepository extends JpaRepository<DayDistrictEntity, DayDistrictId> {
    @EntityGraph(attributePaths = "district")
    @Query("select a from DayDistrictEntity a where a.id.day >= :start and a.id.day < :end order by a.id.day, a.district.name")
    List<DayDistrictEntity> findByDayGreaterThanEqualAndDayLessThan(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = "district")
    @Query("select a from DayDistrictEntity a where a.id.day = :day order by a.district.name")
    List<DayDistrictEntity> findByDay(@Param("day") LocalDate day);
}
