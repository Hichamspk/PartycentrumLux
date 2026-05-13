package nl.partycentrum.lux.dto.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRequest(
        @NotNull Long bookingId,
        @DecimalMin("0.00") @NotNull BigDecimal amount,
        @NotNull LocalDate dueDate,
        InvoiceType invoiceType
) {
}
