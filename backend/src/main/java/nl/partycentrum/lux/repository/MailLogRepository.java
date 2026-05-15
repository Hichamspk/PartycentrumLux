package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.MailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailLogRepository extends JpaRepository<MailLog, Long> {
    List<MailLog> findByBookingIdOrderByVerzondenOpDesc(Long bookingId);

    List<MailLog> findByBezichtigingIdOrderByVerzondenOpDesc(Long bezichtigingId);

    List<MailLog> findAllByOrderByVerzondenOpDesc();
}
