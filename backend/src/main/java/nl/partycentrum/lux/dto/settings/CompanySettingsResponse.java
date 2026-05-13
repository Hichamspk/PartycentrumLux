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
        String docusealHussainEmail,
        String docusealHussainSignatureToken,
        String googleReviewUrl,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        String smtpFrom,
        String generalTerms,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
