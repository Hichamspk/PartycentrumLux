package nl.partycentrum.lux.dto.customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String naam,
        String name,
        String email,
        String telefoon,
        String phone,
        String adres,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
