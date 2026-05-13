package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnuleringService {

    private final BookingService bookingService;
    private final MailService mailService;

    public AnnuleringService(
            BookingService bookingService,
            MailService mailService
    ) {
        this.bookingService = bookingService;
        this.mailService = mailService;
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Annuleringsreden is verplicht.");
        }
        var booking = bookingService.getBooking(bookingId);
        booking.setStatus(BookingStatus.GEANNULEERD);
        booking.setAnnuleringsReden(reason.trim());
        mailService.sendCancellation(booking);
        return bookingService.toResponse(booking);
    }
}
