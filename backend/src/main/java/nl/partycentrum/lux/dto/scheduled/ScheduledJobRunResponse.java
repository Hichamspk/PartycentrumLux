package nl.partycentrum.lux.dto.scheduled;

import java.time.LocalDate;

public record ScheduledJobRunResponse(
        boolean ok,
        LocalDate runDate,
        String message
) {
}
