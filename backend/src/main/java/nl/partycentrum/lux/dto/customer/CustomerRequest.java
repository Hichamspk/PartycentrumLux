package nl.partycentrum.lux.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String phone,
        String address
) {
}
