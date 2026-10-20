package sg.tracker.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisruptionRepository extends JpaRepository<Disruption, Long> {

    Optional<Disruption> findFirstByFingerprintAndActiveTrue(String fingerprint);

    List<Disruption> findByActiveTrue();

    List<Disruption> findByActiveTrueAndLineIgnoreCase(String line);

    List<Disruption> findByLineIgnoreCaseOrderByFirstSeenDesc(String line);

    List<Disruption> findAllByOrderByFirstSeenDesc();
}
