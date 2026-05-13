package nl.partycentrum.lux.dto.invoice;

import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.domain.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        Long bookingId,
        String customerName,
        String invoiceNumber,
        InvoiceType invoiceType,
        LocalDate invoiceDate,
        BigDecimal amount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        InvoiceStatus status,
        LocalDate dueDate,
        LocalDate paidDate,
        String pdfPath,
        String downloadUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
