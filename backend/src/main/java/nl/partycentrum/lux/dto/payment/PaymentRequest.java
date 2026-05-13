package nl.partycentrum.lux.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull Long invoiceId,
        @DecimalMin("0.01") @NotNull BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentMethod paymentMethod,
        String notes
) {
}
