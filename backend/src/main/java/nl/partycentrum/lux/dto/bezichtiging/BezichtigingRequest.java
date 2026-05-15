package nl.partycentrum.lux.dto.bezichtiging;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.BezichtigingStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record BezichtigingRequest(
        @NotBlank String klantNaam,
        @NotBlank @Email String klantEmail,
        @NotBlank String klantTelefoon,
        @NotNull LocalDate datum,
        @NotNull LocalTime startTijd,
        @NotNull LocalTime eindTijd,
        BezichtigingStatus status,
        String notities,
        Long bookingId
) {
}
