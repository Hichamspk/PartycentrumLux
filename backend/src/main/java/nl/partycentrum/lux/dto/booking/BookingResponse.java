package nl.partycentrum.lux.dto.booking;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.ContractStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.dto.subprijs.SubPrijsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        String customerAddress,
        LocalDate eventDate,
        LocalDate date,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        EventType eventType,
        int guestCount,
        BigDecimal price,
        List<SubPrijsResponse> subPrijzen,
        BookingStatus status,
        String notes,
        List<String> properties,
        String conditions,
        ContractStatus contractStatus,
        String docusealSubmissionId,
        LocalDate contractSignedDate,
        String annuleringsReden,
        Long invoiceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
