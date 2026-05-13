package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.SubPrijs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubPrijsRepository extends JpaRepository<SubPrijs, Long> {
    List<SubPrijs> findByBookingIdOrderByPositionAscIdAsc(Long bookingId);
}
