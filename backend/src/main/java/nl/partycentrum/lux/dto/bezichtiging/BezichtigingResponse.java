package nl.partycentrum.lux.dto.bezichtiging;

import nl.partycentrum.lux.domain.BezichtigingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BezichtigingResponse(
        Long id,
        String klantNaam,
        String klantEmail,
        String klantTelefoon,
        LocalDate datum,
        LocalTime startTijd,
        LocalTime eindTijd,
        BezichtigingStatus status,
        String notities,
        Long bookingId,
        String bookingKlantNaam,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
