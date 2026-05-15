package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.Bezichtiging;
import nl.partycentrum.lux.domain.BezichtigingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BezichtigingRepository extends JpaRepository<Bezichtiging, Long> {
    List<Bezichtiging> findByStatusOrderByDatumAscStartTijdAsc(BezichtigingStatus status);

    List<Bezichtiging> findByDatumGreaterThanEqualOrderByDatumAscStartTijdAsc(LocalDate datum);

    List<Bezichtiging> findByDatumBetweenOrderByDatumAscStartTijdAsc(LocalDate start, LocalDate end);

    List<Bezichtiging> findByDatumAndStatusOrderByStartTijdAsc(LocalDate datum, BezichtigingStatus status);

    List<Bezichtiging> findAllByOrderByDatumAscStartTijdAsc();
}
