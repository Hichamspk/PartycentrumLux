package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByEventDateBetweenOrderByEventDateAsc(LocalDate start, LocalDate end);

    List<Booking> findByCustomerIdOrderByEventDateDesc(Long customerId);

    List<Booking> findByEventDateAndStatus(LocalDate eventDate, BookingStatus status);

    List<Booking> findByEventDateAndStatusIn(LocalDate eventDate, List<BookingStatus> statuses);

    List<Booking> findByStatusAndEventDateBefore(BookingStatus status, LocalDate date);

    List<Booking> findByStatusInOrderByEventDateAsc(List<BookingStatus> statuses);

    List<Booking> findTop5ByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAsc(LocalDate date, BookingStatus status);

    List<Booking> findByStatusNotAndEventDate(
            BookingStatus status,
            LocalDate eventDate
    );

    long countByEventDateBetween(LocalDate start, LocalDate end);

    java.util.Optional<Booking> findByDocusealSubmissionId(String docusealSubmissionId);
}
