package nl.partycentrum.lux.dto.payment;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.domain.PaymentPart;
import nl.partycentrum.lux.domain.PaymentState;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentScheduleResponse(
        Long bookingId,
        Long customerId,
        String klantNaam,
        String customerName,
        String customerEmail,
        LocalDate evenementDatum,
        EventType evenementType,
        BookingStatus bookingStatus,
        PaymentPart type,
        BigDecimal bedrag,
        LocalDate deadline,
        PaymentState status,
        LocalDate betaaldDatum,
        boolean locked
) {
}
