package nl.partycentrum.lux.dto.booking;

import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.BookingStatus;

public record BookingStatusRequest(@NotNull BookingStatus status) {
}
