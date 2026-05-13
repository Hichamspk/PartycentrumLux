package nl.partycentrum.lux.dto.subprijs;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubPrijsRequest(
        Long id,
        @NotBlank String naam,
        @DecimalMin("0.00") @NotNull BigDecimal prijs,
        Integer position
) {
}
