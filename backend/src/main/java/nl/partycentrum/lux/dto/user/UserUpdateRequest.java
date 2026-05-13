package nl.partycentrum.lux.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import nl.partycentrum.lux.domain.Role;

public record UserUpdateRequest(
        String name,
        @Email String email,
        @Size(min = 8) String password,
        Role role
) {
}
