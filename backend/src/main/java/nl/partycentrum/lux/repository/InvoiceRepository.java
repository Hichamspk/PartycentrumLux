package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.Invoice;
import nl.partycentrum.lux.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findFirstByBookingIdOrderByCreatedAtAsc(Long bookingId);

    List<Invoice> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    Optional<Invoice> findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(String prefix);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    List<Invoice> findByStatusAndDueDate(InvoiceStatus status, LocalDate date);

    List<Invoice> findByStatusInAndDueDateBefore(List<InvoiceStatus> statuses, LocalDate date);

    List<Invoice> findTop5ByOrderByCreatedAtDesc();
}
