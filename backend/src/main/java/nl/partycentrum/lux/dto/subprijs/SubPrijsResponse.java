package nl.partycentrum.lux.dto.subprijs;

import java.math.BigDecimal;

public record SubPrijsResponse(
        Long id,
        String naam,
        BigDecimal prijs,
        int position
) {
}
