package nl.partycentrum.lux.dto.dashboard;

import java.math.BigDecimal;

public record ChartPoint(
        String label,
        BigDecimal amount,
        long count
) {
}
