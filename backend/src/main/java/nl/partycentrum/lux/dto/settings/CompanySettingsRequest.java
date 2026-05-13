package nl.partycentrum.lux.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompanySettingsRequest(
        @NotBlank String companyName,
        String logoPath,
        String logoBase64,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String brandColor,
        @NotBlank String address,
        @NotBlank String postalCode,
        @NotBlank String city,
        @NotBlank String kvk,
        @NotBlank String vatNumber,
        @NotBlank String iban,
        @NotBlank String phone,
        @Email @NotBlank String email,
        @NotBlank String website,
        @Email @NotBlank String mailFrom,
        String docusealApiKey,
        @NotBlank String docusealBaseUrl,
        String docusealContractTemplateId,
        @NotBlank String generalTerms
) {
}
