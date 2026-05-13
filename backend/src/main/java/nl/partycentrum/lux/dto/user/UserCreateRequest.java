package nl.partycentrum.lux.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nl.partycentrum.lux.domain.Role;

public record UserCreateRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 8) @NotBlank String password,
        @NotNull Role role
) {
}
