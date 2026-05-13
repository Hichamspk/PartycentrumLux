package nl.partycentrum.lux.dto.payment;

import nl.partycentrum.lux.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        Long bookingId,
        String customerName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
