package nl.partycentrum.lux.dto.auth;

import nl.partycentrum.lux.domain.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String name,
        String email,
        Role role
) {
}
