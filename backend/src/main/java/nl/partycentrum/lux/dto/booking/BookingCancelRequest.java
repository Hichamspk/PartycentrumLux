package nl.partycentrum.lux.dto.booking;

import jakarta.validation.constraints.NotBlank;

public record BookingCancelRequest(@NotBlank String reason) {
}
