package nl.partycentrum.lux.dto.settings;

import java.time.LocalDateTime;

public record CompanySettingsResponse(
        Long id,
        String companyName,
        String logoPath,
        String logoBase64,
        String brandColor,
        String address,
        String postalCode,
        String city,
        String kvk,
        String vatNumber,
        String iban,
        String phone,
        String email,
        String website,
        String mailFrom,
        String docusealApiKey,
        String docusealBaseUrl,
        String docusealContractTemplateId,
        String generalTerms,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
