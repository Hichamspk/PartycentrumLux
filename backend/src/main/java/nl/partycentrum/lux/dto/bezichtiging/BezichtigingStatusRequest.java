package nl.partycentrum.lux.dto.bezichtiging;

import jakarta.validation.constraints.NotNull;
import nl.partycentrum.lux.domain.BezichtigingStatus;

public record BezichtigingStatusRequest(@NotNull BezichtigingStatus status) {
}
