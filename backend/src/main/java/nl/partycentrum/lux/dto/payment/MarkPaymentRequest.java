package nl.partycentrum.lux.dto.payment;

import nl.partycentrum.lux.domain.PaymentPart;

import java.time.LocalDate;

public record MarkPaymentRequest(
        PaymentPart type,
        LocalDate betaaldDatum
) {
}
