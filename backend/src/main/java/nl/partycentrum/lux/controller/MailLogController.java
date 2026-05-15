package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.domain.MailLogStatus;
import nl.partycentrum.lux.domain.MailLogType;
import nl.partycentrum.lux.dto.mail.MailLogResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.service.MailLogService;
import nl.partycentrum.lux.service.MailService;
import nl.partycentrum.lux.service.OfferteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mail-logs")
public class MailLogController {

    private final MailLogService mailLogService;
    private final MailService mailService;
    private final OfferteService offerteService;

    public MailLogController(
            MailLogService mailLogService,
            MailService mailService,
            OfferteService offerteService
    ) {
        this.mailLogService = mailLogService;
        this.mailService = mailService;
        this.offerteService = offerteService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<MailLogResponse> findAll(
            @RequestParam(required = false) MailLogType type,
            @RequestParam(required = false) MailLogStatus status,
            @RequestParam(required = false) String search
    ) {
        return mailLogService.findAll(type, status, search);
    }

    @GetMapping("/booking/{bookingId}")
    public List<MailLogResponse> findByBooking(@PathVariable Long bookingId) {
        return mailLogService.findByBooking(bookingId);
    }

    @GetMapping("/bezichtiging/{bezichtigingId}")
    public List<MailLogResponse> findByBezichtiging(@PathVariable Long bezichtigingId) {
        return mailLogService.findByBezichtiging(bezichtigingId);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('OWNER')")
    public MailLogResponse resend(@PathVariable Long id) {
        var log = mailLogService.get(id);
        if (log.getStatus() != MailLogStatus.MISLUKT) {
            throw new ApiException(HttpStatus.CONFLICT, "Alleen mislukte mails kunnen opnieuw verstuurd worden.");
        }
        if (log.getBookingId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deze mail kan nog niet automatisch opnieuw verstuurd worden.");
        }

        switch (log.getType()) {
            case OFFERTE_VERZONDEN -> offerteService.send(log.getBookingId());
            case BEVESTIGINGSMAIL -> mailService.sendOfferteSignedConfirmationByBookingId(log.getBookingId());
            case BETALING_HERINNERING_AANBETALING -> mailService.sendAanbetalingReminderByBookingId(log.getBookingId());
            case BETALING_HERINNERING_RESTANT -> mailService.sendRestantReminderByBookingId(log.getBookingId());
            case EVENEMENT_HERINNERING -> mailService.sendEventReminderByBookingId(log.getBookingId());
            case REVIEW_VERZOEK -> mailService.sendReviewRequestByBookingId(log.getBookingId());
            case ANNULERING -> mailService.sendCancellationByBookingId(log.getBookingId());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Deze mailsoort wordt later ondersteund voor opnieuw versturen.");
        }

        mailLogService.markResent(id);
        return mailLogService.toResponse(mailLogService.get(id));
    }
}
