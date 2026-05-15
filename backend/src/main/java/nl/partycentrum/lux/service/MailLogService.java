package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.MailLog;
import nl.partycentrum.lux.domain.MailLogStatus;
import nl.partycentrum.lux.domain.MailLogType;
import nl.partycentrum.lux.dto.mail.MailLogResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BezichtigingRepository;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.MailLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MailLogService {

    private final MailLogRepository mailLogRepository;
    private final BookingRepository bookingRepository;
    private final BezichtigingRepository bezichtigingRepository;

    public MailLogService(
            MailLogRepository mailLogRepository,
            BookingRepository bookingRepository,
            BezichtigingRepository bezichtigingRepository
    ) {
        this.mailLogRepository = mailLogRepository;
        this.bookingRepository = bookingRepository;
        this.bezichtigingRepository = bezichtigingRepository;
    }

    @Transactional(readOnly = true)
    public List<MailLogResponse> findAll(MailLogType type, MailLogStatus status, String search) {
        return mailLogRepository.findAllByOrderByVerzondenOpDesc().stream()
                .filter(log -> type == null || log.getType() == type)
                .filter(log -> status == null || log.getStatus() == status)
                .filter(log -> search == null || search.isBlank()
                        || log.getOntvangerEmail().toLowerCase().contains(search.toLowerCase()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MailLogResponse> findByBooking(Long bookingId) {
        return mailLogRepository.findByBookingIdOrderByVerzondenOpDesc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MailLogResponse> findByBezichtiging(Long bezichtigingId) {
        return mailLogRepository.findByBezichtigingIdOrderByVerzondenOpDesc(bezichtigingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MailLog get(Long id) {
        return mailLogRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mail log niet gevonden."));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSent(Long bookingId, Long bezichtigingId, MailLogType type, String to, String subject) {
        save(bookingId, bezichtigingId, type, to, subject, MailLogStatus.VERZONDEN, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailed(Long bookingId, Long bezichtigingId, MailLogType type, String to, String subject, Exception exception) {
        save(bookingId, bezichtigingId, type, to, subject, MailLogStatus.MISLUKT, exception.getMessage());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markResent(Long id) {
        var log = get(id);
        log.setStatus(MailLogStatus.VERZONDEN);
        log.setFoutmelding(null);
        log.setVerzondenOp(LocalDateTime.now());
    }

    private void save(
            Long bookingId,
            Long bezichtigingId,
            MailLogType type,
            String to,
            String subject,
            MailLogStatus status,
            String error
    ) {
        var log = new MailLog();
        log.setBookingId(bookingId);
        log.setBezichtigingId(bezichtigingId);
        log.setType(type);
        log.setOntvangerEmail(to == null || to.isBlank() ? "-" : to);
        log.setOnderwerp(subject == null || subject.isBlank() ? "-" : subject);
        log.setStatus(status);
        log.setFoutmelding(error);
        log.setVerzondenOp(LocalDateTime.now());
        mailLogRepository.save(log);
    }

    public MailLogResponse toResponse(MailLog log) {
        return new MailLogResponse(
                log.getId(),
                log.getBookingId(),
                log.getBezichtigingId(),
                log.getType(),
                klantNaam(log),
                log.getOntvangerEmail(),
                log.getOnderwerp(),
                log.getStatus(),
                log.getFoutmelding(),
                log.getVerzondenOp()
        );
    }

    private String klantNaam(MailLog log) {
        if (log.getBookingId() == null) {
            if (log.getBezichtigingId() == null) {
                return null;
            }
            return bezichtigingRepository.findById(log.getBezichtigingId())
                    .map(bezichtiging -> bezichtiging.getKlantNaam())
                    .orElse(null);
        }
        return bookingRepository.findById(log.getBookingId())
                .map(Booking::getCustomer)
                .map(customer -> customer.getNaam())
                .orElse(null);
    }
}
