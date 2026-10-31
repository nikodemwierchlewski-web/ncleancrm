package pl.nclean.crm.calendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<DistrictEntity, Long> {
    List<DistrictEntity> findByActiveTrueOrderByNameAsc();
    List<DistrictEntity> findAllByOrderByActiveDescNameAsc();
    Optional<DistrictEntity> findByIdAndActiveTrue(Long id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
