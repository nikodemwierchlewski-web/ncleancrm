package pl.nclean.crm.calendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<DistrictEntity, Long> {
    List<DistrictEntity> findByActiveTrueOrderByNameAsc();
    Optional<DistrictEntity> findByIdAndActiveTrue(Long id);
}
