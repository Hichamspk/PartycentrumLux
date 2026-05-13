package nl.partycentrum.lux.dto.booking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.dto.subprijs.SubPrijsRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingRequest(
        @NotNull Long customerId,
        LocalDate eventDate,
        LocalDate date,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull EventType eventType,
        @Min(1) int guestCount,
        @DecimalMin("0.00") @NotNull BigDecimal price,
        BookingStatus status,
        String notes,
        List<String> properties,
        String conditions,
        List<SubPrijsRequest> subPrijzen
) {
}
