package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.CompanySettings;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContractTemplateService {

    private static final Locale NL = new Locale("nl", "NL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern VARIABLE = Pattern.compile("\\[\\[\\s*([a-zA-Z0-9_]+)\\s*]]");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.21");

    public String template() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("templates/contract-template.html").getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Contracttemplate kon niet worden geladen.", exception);
        }
    }

    public String render(Booking booking, CompanySettings settings) {
        return render(template(), variables(booking, settings));
    }

    public Map<String, String> variables(Booking booking, CompanySettings settings) {
        var price = booking.getPrice();
        var vat = price.multiply(VAT_RATE);
        var total = price.add(vat);
        return Map.ofEntries(
                Map.entry("logo_src", defaultIfBlank(settings.getLogoBase64(), "")),
                Map.entry("logo_image_display", defaultIfBlank(settings.getLogoBase64(), "").isBlank() ? "none" : "block"),
                Map.entry("logo_fallback_display", defaultIfBlank(settings.getLogoBase64(), "").isBlank() ? "block" : "none"),
                Map.entry("brand_color", defaultIfBlank(settings.getBrandColor(), "#C9A84C")),
                Map.entry("bedrijf_naam", settings.getCompanyName()),
                Map.entry("bedrijf_adres", settings.getAddress()),
                Map.entry("bedrijf_postcode", settings.getPostalCode()),
                Map.entry("bedrijf_stad", settings.getCity()),
                Map.entry("kvk", settings.getKvk()),
                Map.entry("btw_nummer", settings.getVatNumber()),
                Map.entry("telefoon", settings.getPhone()),
                Map.entry("email", settings.getEmail()),
                Map.entry("website", settings.getWebsite()),
                Map.entry("contract_datum", LocalDate.now().format(DATE)),
                Map.entry("contract_nummer", contractNumber(booking)),
                Map.entry("klant_naam", booking.getCustomer().getName()),
                Map.entry("klant_adres", defaultIfBlank(booking.getCustomer().getAddress(), "")),
                Map.entry("klant_email", booking.getCustomer().getEmail()),
                Map.entry("klant_telefoon", booking.getCustomer().getPhone()),
                Map.entry("evenement_datum", booking.getEventDate().format(DATE)),
                Map.entry("start_tijd", booking.getStartTime().format(TIME)),
                Map.entry("eind_tijd", booking.getEndTime().format(TIME)),
                Map.entry("evenement_type", booking.getEventType().name()),
                Map.entry("aantal_gasten", String.valueOf(booking.getGuestCount())),
                Map.entry("eigenschappen_lijst", propertiesList(booking)),
                Map.entry("prijs_excl_btw", money(price)),
                Map.entry("btw_bedrag", money(vat)),
                Map.entry("prijs_incl_btw", money(total)),
                Map.entry("iban", settings.getIban()),
                Map.entry("algemene_voorwaarden", textBlock(settings.getGeneralTerms())),
                Map.entry("extra_voorwaarden", textBlock(defaultIfBlank(booking.getConditions(), "Geen aanvullende voorwaarden.")))
        );
    }

    public String contractNumber(Booking booking) {
        return "LUX-CONTRACT-" + booking.getEventDate().getYear() + "-" + String.format("%04d", booking.getId() == null ? 0 : booking.getId());
    }

    private String render(String template, Map<String, String> variables) {
        var matcher = VARIABLE.matcher(template);
        var rendered = new StringBuilder();
        while (matcher.find()) {
            var key = matcher.group(1);
            var rawHtmlKeys = key.equals("eigenschappen_lijst") || key.equals("algemene_voorwaarden") || key.equals("extra_voorwaarden");
            var value = variables.getOrDefault(key, "");
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(rawHtmlKeys ? value : escapeHtml(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String propertiesList(Booking booking) {
        if (booking.getProperties().isEmpty()) {
            return "<p>Geen specifieke eigenschappen vastgelegd.</p>";
        }
        return "<ul>" + booking.getProperties().stream()
                .map(property -> "<li>" + escapeHtml(property) + "</li>")
                .reduce("", String::concat) + "</ul>";
    }

    private String textBlock(String text) {
        return escapeHtml(text).replace("\n", "<br />");
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(NL)
                .format(amount)
                .replace("\u00A0", " ")
                .replace("€", "EUR");
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
