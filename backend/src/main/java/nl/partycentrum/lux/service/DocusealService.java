package nl.partycentrum.lux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.CompanySettings;
import nl.partycentrum.lux.dto.contract.DocusealSubmissionResult;
import nl.partycentrum.lux.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DocusealService {

    private static final Logger log = LoggerFactory.getLogger(DocusealService.class);

    private final CompanySettingsService companySettingsService;
    private final ContractTemplateService contractTemplateService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DocusealService(
            CompanySettingsService companySettingsService,
            ContractTemplateService contractTemplateService,
            RestClient.Builder builder,
            ObjectMapper objectMapper
    ) {
        this.companySettingsService = companySettingsService;
        this.contractTemplateService = contractTemplateService;
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public DocusealSubmissionResult sendOfferte(Booking booking, byte[] pdfBytes, String documentRef) {
        var settings = companySettingsService.resolve();
        ensureConfigured(settings);
        ensureAutoSignerConfigured(settings);

        var docusealUrl = endpoint(settings.getDocusealBaseUrl(), "/api/submissions");
        var customer = booking.getCustomer();
        var pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

        var verhuurder = new LinkedHashMap<String, Object>();
        verhuurder.put("role", "Verhuurder");
        verhuurder.put("email", settings.getDocusealHussainEmail());
        verhuurder.put("name", "Hussain Siddiqui");
        verhuurder.put("completed", true);
        verhuurder.put("values", Map.of("signature", settings.getDocusealHussainSignatureToken()));

        var huurder = new LinkedHashMap<String, Object>();
        huurder.put("role", "Huurder");
        huurder.put("email", customer.getEmail());
        huurder.put("name", customer.getNaam());
        huurder.put("message", Map.of(
                "subject", "Uw offerte - Partycentrum Lux",
                "body", "Geachte " + customer.getNaam() + ",\n\nHierbij ontvangt u uw offerte van Partycentrum Lux. Klik op de onderstaande knop om de offerte te bekijken en digitaal te ondertekenen.\n\nMet vriendelijke groet,\nHussain Siddiqui\nPartycentrum Lux"
        ));

        var body = new LinkedHashMap<String, Object>();
        body.put("send_email", true);
        if (settings.getDocusealContractTemplateId() != null && !settings.getDocusealContractTemplateId().isBlank()) {
            body.put("template_id", templateId(settings.getDocusealContractTemplateId()));
            verhuurder.put("send_email", false);
            huurder.put("send_email", true);
            verhuurder.put("values", Map.of(
                    "signature", settings.getDocusealHussainSignatureToken(),
                    "Handtekening verhuurder", settings.getDocusealHussainSignatureToken()
            ));
        } else {
            var document = new LinkedHashMap<String, Object>();
            document.put("name", "Offerte-" + documentRef);
            document.put("file", pdfBase64);
            body.put("documents", List.of(document));
        }
        body.put("submitters", List.of(verhuurder, huurder));

        try {
            var response = postForObject(docusealUrl, settings.getDocusealApiKey(), body);
            var submissionId = extractSubmissionId(response);
            if (submissionId == null || submissionId.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DocuSeal gaf geen submission id terug.");
            }
            return new DocusealSubmissionResult(submissionId, Objects.toString(extractSigningUrl(response), ""));
        } catch (ApiException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Offerte kon niet naar DocuSeal worden verstuurd: " + exception.getMessage());
        }
    }

    public DocusealSubmissionResult sendContract(Booking booking, String html) {
        var settings = companySettingsService.resolve();
        ensureConfigured(settings);
        if (settings.getDocusealContractTemplateId() == null || settings.getDocusealContractTemplateId().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DocuSeal Open Source ondersteunt /api/submissions/html niet. Maak eerst een contracttemplate in DocuSeal, vul het template ID in bij Instellingen, of gebruik DocuSeal Pro voor HTML submissions."
            );
        }
        var docusealUrl = endpoint(settings.getDocusealBaseUrl(), "/api/submissions");
        var customer = booking.getCustomer();
        var templateId = templateId(settings.getDocusealContractTemplateId());
        var template = getTemplate(settings, templateId);
        var variables = docusealVariables(booking, settings);
        var prefill = prefillFields(template, variables);
        var role = submitterRole(template);

        var submitter = new LinkedHashMap<String, Object>();
        submitter.put("role", role);
        submitter.put("email", customer.getEmail());
        submitter.put("name", customer.getName());
        submitter.put("external_id", "booking-" + booking.getId());
        submitter.put("values", prefill.values());
        submitter.put("fields", prefill.fields());

        var body = new LinkedHashMap<String, Object>();
        body.put("template_id", templateId);
        body.put("send_email", false);
        body.put("variables", variables);
        body.put("message", Map.of(
                "subject", "Uw contract - Partycentrum Lux",
                "body", "Geachte " + customer.getName() + ",\n\nhierbij ontvangt u uw contract. Klik op de link om het te bekijken en digitaal te ondertekenen."
        ));
        body.put("submitters", List.of(submitter));

        try {
            var response = postForObject(docusealUrl, settings.getDocusealApiKey(), body);
            var submissionId = extractSubmissionId(response);
            if (submissionId == null || submissionId.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DocuSeal gaf geen submission id terug.");
            }
            var signingUrl = extractSigningUrl(response);
            if (signingUrl == null || signingUrl.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DocuSeal gaf geen ondertekenlink terug.");
            }
            return new DocusealSubmissionResult(submissionId, signingUrl);
        } catch (ApiException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Contract kon niet naar DocuSeal worden verstuurd: " + exception.getMessage());
        }
    }

    public boolean testConnection() {
        var settings = companySettingsService.resolve();
        ensureConfigured(settings);
        var docusealUrl = endpoint(settings.getDocusealBaseUrl(), "/api/templates");
        try {
            restClient.get()
                    .uri(docusealUrl)
                    .header("X-Auth-Token", settings.getDocusealApiKey())
                    .exchange((request, clientResponse) -> {
                        var responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.info("DocuSeal response status: {}", clientResponse.getStatusCode().value());
                        log.info("DocuSeal response body: {}", responseBody);
                        if (clientResponse.getStatusCode().isError()) {
                            throw new ApiException(
                                    HttpStatus.BAD_GATEWAY,
                                    "DocuSeal API fout (" + clientResponse.getStatusCode().value() + "): " + responseBody
                            );
                        }
                        return null;
                    });
            return true;
        } catch (ApiException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("DocuSeal error: {}", exception.getMessage(), exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DocuSeal verbinding mislukt. Controleer de URL en API key.");
        }
    }

    private void ensureConfigured(CompanySettings settings) {
        if (settings.getDocusealApiKey() == null || settings.getDocusealApiKey().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DocuSeal API key ontbreekt in Instellingen.");
        }
        if (settings.getDocusealBaseUrl() == null || settings.getDocusealBaseUrl().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DocuSeal base URL ontbreekt in Instellingen.");
        }
    }

    private void ensureAutoSignerConfigured(CompanySettings settings) {
        if (settings.getDocusealHussainEmail() == null || settings.getDocusealHussainEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hussain e-mailadres ontbreekt in Instellingen.");
        }
        if (settings.getDocusealHussainSignatureToken() == null || settings.getDocusealHussainSignatureToken().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hussain signature token ontbreekt in Instellingen.");
        }
    }

    private String endpoint(String docusealBaseUrl, String path) {
        var baseUrl = docusealBaseUrl.endsWith("/")
                ? docusealBaseUrl.substring(0, docusealBaseUrl.length() - 1)
                : docusealBaseUrl;
        log.info("DocuSeal URL: {}", baseUrl + path);
        return baseUrl + path;
    }

    private Object postForObject(String docusealUrl, String apiKey, Map<String, Object> body) {
        log.info("DocuSeal request body: {}", toJson(body));
        return restClient.post()
                .uri(docusealUrl)
                .header("X-Auth-Token", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, clientResponse) -> {
                    var responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.info("DocuSeal response status: {}", clientResponse.getStatusCode().value());
                    log.info("DocuSeal response body: {}", responseBody);
                    if (clientResponse.getStatusCode().isError()) {
                        var status = clientResponse.getStatusCode().is4xxClientError()
                                ? HttpStatus.BAD_REQUEST
                                : HttpStatus.BAD_GATEWAY;
                        throw new ApiException(
                                status,
                                "DocuSeal API fout (" + clientResponse.getStatusCode().value() + "): " + responseBody
                        );
                    }
                    if (responseBody.isBlank()) {
                        return Map.of();
                    }
                    return objectMapper.readValue(responseBody, Object.class);
                });
    }

    private Object getTemplate(CompanySettings settings, Long templateId) {
        var docusealUrl = endpoint(settings.getDocusealBaseUrl(), "/api/templates/" + templateId);
        return restClient.get()
                .uri(docusealUrl)
                .header("X-Auth-Token", settings.getDocusealApiKey())
                .exchange((request, clientResponse) -> {
                    var responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.info("DocuSeal response status: {}", clientResponse.getStatusCode().value());
                    log.info("DocuSeal response body: {}", responseBody);
                    if (clientResponse.getStatusCode().isError()) {
                        throw new ApiException(
                                HttpStatus.BAD_GATEWAY,
                                "DocuSeal template kon niet worden opgehaald (" + clientResponse.getStatusCode().value() + "): " + responseBody
                        );
                    }
                    if (responseBody.isBlank()) {
                        return Map.of();
                    }
                    return objectMapper.readValue(responseBody, Object.class);
                });
    }

    private Map<String, String> docusealVariables(Booking booking, CompanySettings settings) {
        var variables = new LinkedHashMap<>(contractTemplateService.variables(booking, settings));
        variables.put("eigenschappen_lijst", booking.getProperties().isEmpty()
                ? "Geen specifieke eigenschappen vastgelegd."
                : String.join("\n", booking.getProperties()));
        variables.put("algemene_voorwaarden", plainText(settings.getGeneralTerms()));
        variables.put("extra_voorwaarden", plainText(
                booking.getConditions() == null || booking.getConditions().isBlank()
                        ? "Geen aanvullende voorwaarden."
                        : booking.getConditions()
        ));
        variables.put("klant_postcode", "");
        variables.put("klant_stad", "");
        variables.put("zaal_naam", "Partycentrum Lux");
        variables.put("subprijs_naam_1", "Huur evenementenlocatie");
        variables.put("subprijs_bedrag_1", variables.get("prijs_excl_btw"));
        variables.put("subprijs_naam_2", "");
        variables.put("subprijs_bedrag_2", "");
        variables.put("kvk_nummer", variables.get("kvk"));
        return variables;
    }

    private PrefillFields prefillFields(Object template, Map<String, String> variables) {
        var templateFields = templateFields(template);
        var values = new LinkedHashMap<String, String>();
        var fields = new ArrayList<Map<String, Object>>();

        for (var templateField : templateFields) {
            if (!isPrefillable(templateField)) {
                continue;
            }
            var value = valueForTemplateField(templateField.name(), variables);
            if (value == null) {
                continue;
            }
            var defaultValue = value.isBlank() ? "." : value;
            values.put(templateField.name(), defaultValue);
            fields.add(Map.of(
                    "name", templateField.name(),
                    "default_value", defaultValue,
                    "readonly", true
            ));
        }

        if (fields.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DocuSeal template bevat geen invulbare tekstvelden die bij de boeking passen. " +
                            "Voeg in DocuSeal tekstvelden toe met namen zoals klant_naam, evenement_datum, start_tijd, eind_tijd, prijs_excl_btw, btw_bedrag, prijs_incl_btw en contract_nummer. " +
                            "Gevonden velden: " + describeTemplateFields(templateFields)
            );
        }

        log.info("DocuSeal prefill fields: {}", values.keySet());
        return new PrefillFields(values, fields);
    }

    private List<TemplateField> templateFields(Object template) {
        if (!(template instanceof Map<?, ?> map)) {
            return List.of();
        }
        var rawFields = map.get("fields");
        if (!(rawFields instanceof List<?> fields)) {
            return List.of();
        }
        var result = new ArrayList<TemplateField>();
        for (var rawField : fields) {
            if (rawField instanceof Map<?, ?> field) {
                var name = stringValue(field.get("name"));
                var type = stringValue(field.get("type"));
                result.add(new TemplateField(name, type));
            }
        }
        return result;
    }

    private boolean isPrefillable(TemplateField field) {
        if (field.name() == null || field.name().isBlank()) {
            return false;
        }
        var type = field.type() == null ? "" : field.type().toLowerCase();
        return !List.of("signature", "initials", "file", "image", "stamp", "payment", "verification", "kba")
                .contains(type);
    }

    private String valueForTemplateField(String fieldName, Map<String, String> variables) {
        var normalized = normalize(fieldName);
        var aliases = fieldAliases();
        var variableKey = aliases.getOrDefault(normalized, normalized);
        var direct = variables.get(variableKey);
        if (direct != null) {
            return plainText(direct);
        }

        for (var entry : variables.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return plainText(entry.getValue());
            }
        }
        return null;
    }

    private Map<String, String> fieldAliases() {
        return Map.ofEntries(
                Map.entry("klant", "klant_naam"),
                Map.entry("klantnaam", "klant_naam"),
                Map.entry("naamhuurder", "klant_naam"),
                Map.entry("huurder", "klant_naam"),
                Map.entry("customername", "klant_naam"),
                Map.entry("klantadres", "klant_adres"),
                Map.entry("adresklant", "klant_adres"),
                Map.entry("huurderadres", "klant_adres"),
                Map.entry("klantpostcode", "klant_postcode"),
                Map.entry("klantstad", "klant_stad"),
                Map.entry("klantemail", "klant_email"),
                Map.entry("emailklant", "klant_email"),
                Map.entry("huurderemail", "klant_email"),
                Map.entry("klanttelefoon", "klant_telefoon"),
                Map.entry("telefoonklant", "klant_telefoon"),
                Map.entry("huurdertelefoon", "klant_telefoon"),
                Map.entry("evenementdatum", "evenement_datum"),
                Map.entry("eventdatum", "evenement_datum"),
                Map.entry("datum", "evenement_datum"),
                Map.entry("begintijd", "start_tijd"),
                Map.entry("starttijd", "start_tijd"),
                Map.entry("eindtijd", "eind_tijd"),
                Map.entry("eindetijd", "eind_tijd"),
                Map.entry("evenementtype", "evenement_type"),
                Map.entry("eventtype", "evenement_type"),
                Map.entry("type", "evenement_type"),
                Map.entry("aantalgasten", "aantal_gasten"),
                Map.entry("gasten", "aantal_gasten"),
                Map.entry("gastenaantal", "aantal_gasten"),
                Map.entry("huurprijs", "prijs_excl_btw"),
                Map.entry("prijs", "prijs_excl_btw"),
                Map.entry("bedrag", "prijs_excl_btw"),
                Map.entry("subtotaal", "prijs_excl_btw"),
                Map.entry("prijsexclbtw", "prijs_excl_btw"),
                Map.entry("exclbtw", "prijs_excl_btw"),
                Map.entry("btw", "btw_bedrag"),
                Map.entry("btwbedrag", "btw_bedrag"),
                Map.entry("totaal", "prijs_incl_btw"),
                Map.entry("totaalbedrag", "prijs_incl_btw"),
                Map.entry("prijsinclbtw", "prijs_incl_btw"),
                Map.entry("inclbtw", "prijs_incl_btw"),
                Map.entry("contractdatum", "contract_datum"),
                Map.entry("contractdatump2", "contract_datum"),
                Map.entry("contractnummer", "contract_nummer"),
                Map.entry("contractnummerp2", "contract_nummer"),
                Map.entry("contractnummerp2body", "contract_nummer"),
                Map.entry("contractnr", "contract_nummer"),
                Map.entry("bedrijfsnaam", "bedrijf_naam"),
                Map.entry("verhuurder", "bedrijf_naam"),
                Map.entry("bedrijfsadres", "bedrijf_adres"),
                Map.entry("postcode", "bedrijf_postcode"),
                Map.entry("stad", "bedrijf_stad"),
                Map.entry("zaalnaam", "zaal_naam"),
                Map.entry("subprijsnaam1", "subprijs_naam_1"),
                Map.entry("subprijsbedrag1", "subprijs_bedrag_1"),
                Map.entry("subprijsnaam2", "subprijs_naam_2"),
                Map.entry("subprijsbedrag2", "subprijs_bedrag_2"),
                Map.entry("btwnummer", "btw_nummer"),
                Map.entry("btwnr", "btw_nummer"),
                Map.entry("kvknummer", "kvk_nummer"),
                Map.entry("ibanbetaling", "iban"),
                Map.entry("telefoon", "telefoon"),
                Map.entry("email", "email"),
                Map.entry("eigenschappen", "eigenschappen_lijst"),
                Map.entry("algemenevoorwaarden", "algemene_voorwaarden"),
                Map.entry("voorwaarden", "algemene_voorwaarden"),
                Map.entry("aanvullendevoorwaarden", "extra_voorwaarden"),
                Map.entry("extravoorwaarden", "extra_voorwaarden")
        );
    }

    private String submitterRole(Object template) {
        if (template instanceof Map<?, ?> map && map.get("submitters") instanceof List<?> submitters) {
            for (var submitter : submitters) {
                if (submitter instanceof Map<?, ?> submitterMap) {
                    var name = stringValue(submitterMap.get("name"));
                    if ("Huurder".equalsIgnoreCase(name)) {
                        return name;
                    }
                }
            }
            for (var submitter : submitters) {
                if (submitter instanceof Map<?, ?> submitterMap) {
                    var name = stringValue(submitterMap.get("name"));
                    if (name != null && !name.isBlank()) {
                        log.info("DocuSeal template gebruikt rol '{}' voor de huurder.", name);
                        return name;
                    }
                }
            }
        }
        return "Huurder";
    }

    private String describeTemplateFields(List<TemplateField> fields) {
        if (fields.isEmpty()) {
            return "geen";
        }
        return fields.stream()
                .map(field -> {
                    var name = field.name() == null || field.name().isBlank() ? "(zonder naam)" : field.name();
                    var type = field.type() == null || field.type().isBlank() ? "onbekend" : field.type();
                    return name + " [" + type + "]";
                })
                .reduce((left, right) -> left + ", " + right)
                .orElse("geen");
    }

    private Long templateId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DocuSeal template ID moet een numerieke waarde zijn.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(String value) {
        return Normalizer.normalize(Objects.toString(value, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
    }

    private String plainText(String value) {
        return Objects.toString(value, "")
                .replace("<br />", "\n")
                .replace("<br/>", "\n")
                .replace("<br>", "\n")
                .replaceAll("<li>", "- ")
                .replaceAll("</li>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&nbsp;", " ")
                .replace("&#39;", "'")
                .trim();
    }

    private String extractSubmissionId(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (var key : List.of("submission_id", "submissionId", "id")) {
                var direct = map.get(key);
                if (direct != null) {
                    return String.valueOf(direct);
                }
            }
            for (var nested : map.values()) {
                var found = extractSubmissionId(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (var item : list) {
                var found = extractSubmissionId(item);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String extractSigningUrl(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (var key : List.of("embed_src", "embedSrc", "signing_url", "signingUrl", "url")) {
                var direct = map.get(key);
                if (direct != null) {
                    return String.valueOf(direct);
                }
            }
            for (var nested : map.values()) {
                var found = extractSigningUrl(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (var item : list) {
                var found = extractSigningUrl(item);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private record TemplateField(String name, String type) {
    }

    private record PrefillFields(Map<String, String> values, List<Map<String, Object>> fields) {
    }
}
