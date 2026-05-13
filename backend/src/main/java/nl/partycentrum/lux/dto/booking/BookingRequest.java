package nl.partycentrum.lux.dto.booking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.dto.customer.CustomerRequest;
import nl.partycentrum.lux.dto.subprijs.SubPrijsRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingRequest(
        Long customerId,
        CustomerRequest customer,
        LocalDate evenementDatum,
        LocalDate eventDate,
        LocalDate date,
        LocalDate endDate,
        LocalTime startTijd,
        LocalTime startTime,
        LocalTime eindTijd,
        LocalTime endTime,
        EventType evenementType,
        EventType eventType,
        @Min(1) Integer aantalGasten,
        @Min(1) Integer guestCount,
        @DecimalMin("0.00") BigDecimal price,
        @DecimalMin("0.00") BigDecimal korting,
        @Min(0) Integer aanbetalingPercentage,
        BookingStatus status,
        String notes,
        List<String> eigenschappen,
        List<String> properties,
        String conditions,
        List<SubPrijsRequest> subPrijzen
) {
    public LocalDate resolvedEvenementDatum() {
        if (evenementDatum != null) {
            return evenementDatum;
        }
        if (eventDate != null) {
            return eventDate;
        }
        return date;
    }

    public LocalTime resolvedStartTijd() {
        return startTijd != null ? startTijd : startTime;
    }

    public LocalTime resolvedEindTijd() {
        return eindTijd != null ? eindTijd : endTime;
    }

    public EventType resolvedEvenementType() {
        return evenementType != null ? evenementType : eventType;
    }

    public Integer resolvedAantalGasten() {
        return aantalGasten != null ? aantalGasten : guestCount;
    }

    public List<String> resolvedEigenschappen() {
        return eigenschappen != null ? eigenschappen : properties;
    }
}
