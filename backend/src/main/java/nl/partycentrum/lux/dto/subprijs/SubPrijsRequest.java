package nl.partycentrum.lux.dto.subprijs;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record SubPrijsRequest(
        Long id,
        @NotBlank String naam,
        @DecimalMin("0.00") BigDecimal bedrag,
        @DecimalMin("0.00") BigDecimal prijs,
        Integer position
) {
    public BigDecimal resolvedBedrag() {
        return bedrag != null ? bedrag : prijs;
    }
}
