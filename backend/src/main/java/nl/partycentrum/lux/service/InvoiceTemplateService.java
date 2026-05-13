package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.CompanySettings;
import nl.partycentrum.lux.domain.Invoice;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class InvoiceTemplateService {

    private static final Locale NL = new Locale("nl", "NL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9]+)\\s*}}");
    private static final String FALLBACK_COLOR = "#C9A84C";

    public String template() {
        try {
            var resource = new ClassPathResource("templates/invoice-template.html");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Factuurtemplate kon niet worden geladen.", exception);
        }
    }

    public String render(Invoice invoice, CompanySettings settings) {
        return render(template(), variables(invoice, settings));
    }

    public String renderPreview(Map<String, String> settings) {
        var variables = new HashMap<String, String>();
        variables.put("brandColor", defaultIfBlank(settings.get("brandColor"), FALLBACK_COLOR));
        variables.put("brandColorSoft", lighten(variables.get("brandColor"), 0.88));
        variables.put("logoSrc", defaultIfBlank(settings.get("logoBase64"), ""));
        variables.put("logoImageDisplay", variables.get("logoSrc").isBlank() ? "none" : "block");
        variables.put("logoFallbackDisplay", variables.get("logoSrc").isBlank() ? "block" : "none");
        variables.put("companyName", defaultIfBlank(settings.get("companyName"), "Partycentrum Lux"));
        variables.put("companyAddress", defaultIfBlank(settings.get("address"), "Voorbeeldstraat 1"));
        variables.put("companyPostalCode", defaultIfBlank(settings.get("postalCode"), "1000 AA"));
        variables.put("companyCity", defaultIfBlank(settings.get("city"), "Amsterdam"));
        variables.put("companyKvk", defaultIfBlank(settings.get("kvk"), "00000000"));
        variables.put("companyVatNumber", defaultIfBlank(settings.get("vatNumber"), "NL000000000B01"));
        variables.put("companyIban", defaultIfBlank(settings.get("iban"), "NL00 BANK 0000 0000 00"));
        variables.put("companyPhone", defaultIfBlank(settings.get("phone"), "+31 20 000 0000"));
        variables.put("companyEmail", defaultIfBlank(settings.get("email"), "info@partycentrumlux.nl"));
        variables.put("companyWebsite", defaultIfBlank(settings.get("website"), "www.partycentrumlux.nl"));
        variables.put("invoiceNumber", "LUX-2026-001");
        variables.put("invoiceDate", "01-06-2026");
        variables.put("invoiceDueDate", "15-06-2026");
        variables.put("customerName", "Familie De Vries");
        variables.put("customerEmail", "devries@example.com");
        variables.put("customerPhone", "+31 6 12345678");
        variables.put("customerAddress", "Kerkstraat 14, 1017 GM Amsterdam");
        variables.put("eventType", "BRUILOFT");
        variables.put("bookingDate", "14-06-2026");
        variables.put("guestCount", "120");
        variables.put("amount", "EUR 2.500,00");
        variables.put("vatAmount", "EUR 525,00");
        variables.put("totalAmount", "EUR 3.025,00");
        return render(template(), variables);
    }

    private Map<String, String> variables(Invoice invoice, CompanySettings settings) {
        var booking = invoice.getBooking();
        var customer = booking.getCustomer();
        var brandColor = defaultIfBlank(settings.getBrandColor(), FALLBACK_COLOR);
        var logoSrc = defaultIfBlank(settings.getLogoBase64(), "");

        var variables = new HashMap<String, String>();
        variables.put("brandColor", brandColor);
        variables.put("brandColorSoft", lighten(brandColor, 0.88));
        variables.put("logoSrc", logoSrc);
        variables.put("logoImageDisplay", logoSrc.isBlank() ? "none" : "block");
        variables.put("logoFallbackDisplay", logoSrc.isBlank() ? "block" : "none");
        variables.put("companyName", settings.getCompanyName());
        variables.put("companyAddress", settings.getAddress());
        variables.put("companyPostalCode", settings.getPostalCode());
        variables.put("companyCity", settings.getCity());
        variables.put("companyKvk", settings.getKvk());
        variables.put("companyVatNumber", settings.getVatNumber());
        variables.put("companyIban", settings.getIban());
        variables.put("companyPhone", settings.getPhone());
        variables.put("companyEmail", settings.getEmail());
        variables.put("companyWebsite", settings.getWebsite());
        variables.put("invoiceNumber", invoice.getInvoiceNumber());
        variables.put("invoiceDate", invoice.getInvoiceDate().format(DATE));
        variables.put("invoiceDueDate", invoice.getDueDate().format(DATE));
        variables.put("customerName", customer.getName());
        variables.put("customerEmail", customer.getEmail());
        variables.put("customerPhone", customer.getPhone());
        variables.put("customerAddress", defaultIfBlank(customer.getAddress(), ""));
        variables.put("eventType", booking.getEventType().name());
        variables.put("invoiceDescription", invoiceDescription(invoice));
        variables.put("bookingDate", booking.getEventDate().format(DATE));
        variables.put("guestCount", String.valueOf(booking.getGuestCount()));
        variables.put("amount", money(invoice.getAmount()));
        variables.put("vatAmount", money(invoice.getVatAmount()));
        variables.put("totalAmount", money(invoice.getTotalAmount()));
        return variables;
    }

    private String render(String template, Map<String, String> variables) {
        var matcher = VARIABLE.matcher(template);
        var rendered = new StringBuilder();
        while (matcher.find()) {
            var value = variables.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(rendered, java.util.regex.Matcher.quoteReplacement(escapeHtml(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String invoiceDescription(Invoice invoice) {
        var subPrijzen = invoice.getBooking().getSubPrijzen().stream()
                .map(subPrijs -> subPrijs.getNaam())
                .reduce((left, right) -> left + ", " + right)
                .orElse(invoice.getBooking().getEventType().name());
        return switch (invoice.getInvoiceType()) {
            case AANBETALING -> "Aanbetaling 30% - " + subPrijzen;
            case RESTANT -> "Restantbetaling 70% - " + subPrijzen;
            case VOLLEDIG -> "Volledige factuur - " + subPrijzen;
        };
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

    private String lighten(String hex, double factor) {
        var color = defaultIfBlank(hex, FALLBACK_COLOR);
        try {
            var red = Integer.parseInt(color.substring(1, 3), 16);
            var green = Integer.parseInt(color.substring(3, 5), 16);
            var blue = Integer.parseInt(color.substring(5, 7), 16);
            return "#"
                    + channel(red, factor)
                    + channel(green, factor)
                    + channel(blue, factor);
        } catch (RuntimeException exception) {
            return "#F7F1DF";
        }
    }

    private String channel(int value, double factor) {
        var mixed = (int) Math.round(value + (255 - value) * factor);
        return String.format("%02X", Math.min(255, Math.max(0, mixed)));
    }
}
