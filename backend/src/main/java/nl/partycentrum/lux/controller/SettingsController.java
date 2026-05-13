package nl.partycentrum.lux.controller;

import jakarta.validation.Valid;
import nl.partycentrum.lux.dto.settings.CompanySettingsRequest;
import nl.partycentrum.lux.dto.settings.CompanySettingsResponse;
import nl.partycentrum.lux.dto.settings.DocusealConnectionResponse;
import nl.partycentrum.lux.service.CompanySettingsService;
import nl.partycentrum.lux.service.DocusealService;
import nl.partycentrum.lux.service.InvoiceTemplateService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('OWNER')")
public class SettingsController {

    private final CompanySettingsService companySettingsService;
    private final InvoiceTemplateService invoiceTemplateService;
    private final DocusealService docusealService;

    public SettingsController(
            CompanySettingsService companySettingsService,
            InvoiceTemplateService invoiceTemplateService,
            DocusealService docusealService
    ) {
        this.companySettingsService = companySettingsService;
        this.invoiceTemplateService = invoiceTemplateService;
        this.docusealService = docusealService;
    }

    @GetMapping
    public CompanySettingsResponse get() {
        return companySettingsService.get();
    }

    @PutMapping
    public CompanySettingsResponse update(@Valid @RequestBody CompanySettingsRequest request) {
        return companySettingsService.update(request);
    }

    @GetMapping(value = "/invoice-template", produces = MediaType.TEXT_HTML_VALUE)
    public String invoiceTemplate() {
        return invoiceTemplateService.template();
    }

    @PostMapping("/docuseal/test")
    public DocusealConnectionResponse testDocuseal() {
        docusealService.testConnection();
        return new DocusealConnectionResponse(true, "DocuSeal verbinding werkt.");
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanySettingsResponse uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo bestand ontbreekt.");
        }
        var contentType = normalizeContentType(file);
        if (!isAllowedLogo(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alleen PNG, JPG of SVG logo's zijn toegestaan.");
        }
        var logoBase64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        return companySettingsService.updateLogoBase64(logoBase64);
    }

    private String normalizeContentType(MultipartFile file) {
        var contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT);
        }

        var filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private boolean isAllowedLogo(String contentType) {
        return contentType.equals("image/png")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/svg+xml");
    }
}
