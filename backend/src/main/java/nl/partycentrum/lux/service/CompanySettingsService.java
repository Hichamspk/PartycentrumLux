package nl.partycentrum.lux.service;

import nl.partycentrum.lux.config.CompanyProperties;
import nl.partycentrum.lux.config.MailProperties;
import nl.partycentrum.lux.domain.CompanySettings;
import nl.partycentrum.lux.dto.settings.CompanySettingsRequest;
import nl.partycentrum.lux.dto.settings.CompanySettingsResponse;
import nl.partycentrum.lux.repository.CompanySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySettingsService {

    private final CompanySettingsRepository repository;
    private final CompanyProperties companyProperties;
    private final MailProperties mailProperties;

    public CompanySettingsService(
            CompanySettingsRepository repository,
            CompanyProperties companyProperties,
            MailProperties mailProperties
    ) {
        this.repository = repository;
        this.companyProperties = companyProperties;
        this.mailProperties = mailProperties;
    }

    @Transactional
    public CompanySettingsResponse get() {
        return toResponse(resolve());
    }

    @Transactional
    public CompanySettingsResponse update(CompanySettingsRequest request) {
        var settings = resolve();
        settings.setCompanyName(request.companyName());
        settings.setLogoPath(request.logoPath());
        settings.setLogoBase64(request.logoBase64());
        settings.setBrandColor(defaultIfBlank(request.brandColor(), "#C9A84C"));
        settings.setAddress(request.address());
        settings.setPostalCode(request.postalCode());
        settings.setCity(request.city());
        settings.setKvk(request.kvk());
        settings.setVatNumber(request.vatNumber());
        settings.setIban(request.iban());
        settings.setPhone(request.phone());
        settings.setEmail(request.email());
        settings.setWebsite(request.website());
        settings.setMailFrom(request.mailFrom());
        settings.setDocusealApiKey(request.docusealApiKey());
        settings.setDocusealBaseUrl(defaultIfBlank(request.docusealBaseUrl(), "http://lux-docuseal:3000"));
        settings.setDocusealContractTemplateId(blankToNull(request.docusealContractTemplateId()));
        settings.setDocusealHussainEmail(blankToNull(request.docusealHussainEmail()));
        settings.setDocusealHussainSignatureToken(blankToNull(request.docusealHussainSignatureToken()));
        settings.setGoogleReviewUrl(defaultIfBlank(request.googleReviewUrl(), defaultReviewUrl()));
        settings.setSmtpHost(defaultIfBlank(request.smtpHost(), "smtp.gmail.com"));
        settings.setSmtpPort(request.smtpPort() == null ? 587 : request.smtpPort());
        settings.setSmtpUsername(blankToNull(request.smtpUsername()));
        settings.setSmtpPassword(blankToNull(request.smtpPassword()));
        settings.setSmtpFrom(defaultIfBlank(request.smtpFrom(), request.mailFrom()));
        settings.setGeneralTerms(defaultIfBlank(request.generalTerms(), defaultGeneralTerms()));
        return toResponse(settings);
    }

    @Transactional
    public CompanySettingsResponse updateLogoPath(String logoPath) {
        var settings = resolve();
        settings.setLogoPath(logoPath);
        return toResponse(settings);
    }

    @Transactional
    public CompanySettingsResponse updateLogoBase64(String logoBase64) {
        var settings = resolve();
        settings.setLogoBase64(logoBase64);
        return toResponse(settings);
    }

    public CompanySettings resolve() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            var settings = new CompanySettings();
            settings.setCompanyName("Partycentrum Lux");
            settings.setLogoPath(companyProperties.logoPath());
            settings.setLogoBase64(null);
            settings.setBrandColor("#C9A84C");
            settings.setAddress("Voorbeeldstraat 1, 1000 AA Amsterdam");
            settings.setPostalCode("1000 AA");
            settings.setCity("Amsterdam");
            settings.setKvk("00000000");
            settings.setVatNumber("NL000000000B01");
            settings.setIban("NL00 BANK 0000 0000 00");
            settings.setPhone("+31 20 000 0000");
            settings.setEmail("info@partycentrumlux.nl");
            settings.setWebsite("www.partycentrumlux.nl");
            settings.setMailFrom(mailProperties.from());
            settings.setDocusealApiKey(null);
            settings.setDocusealBaseUrl("http://lux-docuseal:3000");
            settings.setDocusealContractTemplateId(null);
            settings.setDocusealHussainEmail(null);
            settings.setDocusealHussainSignatureToken(null);
            settings.setGoogleReviewUrl(defaultReviewUrl());
            settings.setSmtpHost("smtp.gmail.com");
            settings.setSmtpPort(587);
            settings.setSmtpUsername(null);
            settings.setSmtpPassword(null);
            settings.setSmtpFrom(mailProperties.from());
            settings.setGeneralTerms(defaultGeneralTerms());
            return repository.save(settings);
        });
    }

    public CompanySettingsResponse toResponse(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getId(),
                settings.getCompanyName(),
                settings.getLogoPath(),
                settings.getLogoBase64(),
                settings.getBrandColor(),
                settings.getAddress(),
                settings.getPostalCode(),
                settings.getCity(),
                settings.getKvk(),
                settings.getVatNumber(),
                settings.getIban(),
                settings.getPhone(),
                settings.getEmail(),
                settings.getWebsite(),
                settings.getMailFrom(),
                settings.getDocusealApiKey(),
                settings.getDocusealBaseUrl(),
                settings.getDocusealContractTemplateId(),
                settings.getDocusealHussainEmail(),
                settings.getDocusealHussainSignatureToken(),
                settings.getGoogleReviewUrl(),
                settings.getSmtpHost(),
                settings.getSmtpPort(),
                settings.getSmtpUsername(),
                settings.getSmtpPassword(),
                settings.getSmtpFrom(),
                settings.getGeneralTerms(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultGeneralTerms() {
        return "Annulering, schade, geluid en overige afspraken worden conform de algemene voorwaarden van Partycentrum Lux behandeld.";
    }

    private String defaultReviewUrl() {
        return "https://www.google.com/search?q=Partycentrum+Lux+review";
    }
}
