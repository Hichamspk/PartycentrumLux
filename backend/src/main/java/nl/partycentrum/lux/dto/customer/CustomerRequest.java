package nl.partycentrum.lux.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        String naam,
        String name,
        @Email @NotBlank String email,
        String telefoon,
        String phone,
        String adres,
        String address
) {
    public String resolvedNaam() {
        return firstNotBlank(naam, name);
    }

    public String resolvedTelefoon() {
        return firstNotBlank(telefoon, phone);
    }

    public String resolvedAdres() {
        return firstNotBlank(adres, address);
    }

    private String firstNotBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
