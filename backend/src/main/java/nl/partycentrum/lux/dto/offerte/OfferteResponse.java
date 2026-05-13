package nl.partycentrum.lux.dto.offerte;

import nl.partycentrum.lux.domain.BookingStatus;

import java.time.LocalDate;

public record OfferteResponse(
        Long bookingId,
        BookingStatus status,
        String documentRef,
        LocalDate offerteDatum,
        LocalDate offerteSentDate,
        LocalDate ondertekeningDatum,
        String docusealSubmissionId,
        String pdfPath,
        String downloadUrl
) {
}
