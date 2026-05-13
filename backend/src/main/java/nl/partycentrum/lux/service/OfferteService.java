package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.SubPrijs;
import nl.partycentrum.lux.dto.offerte.OfferteResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OfferteService {

    private static final Locale NL = new Locale("nl", "NL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter PAYMENT_REF_DATE = DateTimeFormatter.ofPattern("yyyy-dd-MM");
    private static final Path OFFERTE_DIR = Path.of("offerte");

    private final BookingRepository bookingRepository;
    private final DocusealService docusealService;
    private final MailService mailService;

    public OfferteService(
            BookingRepository bookingRepository,
            DocusealService docusealService,
            MailService mailService
    ) {
        this.bookingRepository = bookingRepository;
        this.docusealService = docusealService;
        this.mailService = mailService;
    }

    @Transactional
    public OfferteResponse generate(Long bookingId) {
        var booking = getBooking(bookingId);
        var pdf = generatePdfBytes(booking);
        var path = savePdf(booking, pdf);
        booking.setOffertePdfPath(path.toString());
        if (booking.getOfferteDatum() == null) {
            booking.setOfferteDatum(LocalDate.now());
        }
        return toResponse(booking);
    }

    @Transactional
    public OfferteResponse send(Long bookingId) {
        var booking = getBooking(bookingId);
        var pdf = generatePdfBytes(booking);
        var path = savePdf(booking, pdf);
        var submission = docusealService.sendOfferte(booking, pdf, documentRef(booking));
        booking.setOffertePdfPath(path.toString());
        booking.setDocusealSubmissionId(submission.submissionId());
        booking.setStatus(BookingStatus.OFFERTE_VERZONDEN);
        booking.setOfferteDatum(booking.getOfferteDatum() == null ? LocalDate.now() : booking.getOfferteDatum());
        booking.setOfferteSentDate(LocalDate.now());
        return toResponse(booking);
    }

    @Transactional
    public OfferteResponse handleDocusealWebhook(Map<String, Object> payload) {
        var submissionId = extractSubmissionId(payload);
        if (submissionId == null || submissionId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DocuSeal webhook bevat geen submission id.");
        }
        var booking = bookingRepository.findByDocusealSubmissionId(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Geen boeking gevonden voor DocuSeal submission."));
        booking.setStatus(BookingStatus.BEVESTIGD);
        booking.setOndertekeningDatum(LocalDate.now());
        mailService.sendOfferteSignedConfirmation(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long bookingId) {
        return generatePdfBytes(getBooking(bookingId));
    }

    @Transactional(readOnly = true)
    public Resource download(Long bookingId) {
        var booking = getBooking(bookingId);
        if (booking.getOffertePdfPath() == null || booking.getOffertePdfPath().isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Voor deze boeking is nog geen offerte-PDF gegenereerd.");
        }
        try {
            var path = Path.of(booking.getOffertePdfPath()).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Offerte-PDF bestand niet gevonden op de server.");
            }
            return new UrlResource(path.toUri());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Offerte download link is ongeldig.");
        }
    }

    public byte[] generatePdfBytes(Booking booking) {
        var html = renderHtml(booking);
        try (var output = new ByteArrayOutputStream()) {
            var renderer = new ITextRenderer();
            renderer.getSharedContext().setPrint(true);
            renderer.getSharedContext().setInteractive(false);
            renderer.setDocumentFromString(toXhtml(html));
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Offerte-PDF kon niet worden gegenereerd: " + exception.getMessage());
        }
    }

    public String renderHtml(Booking booking) {
        var html = loadTemplate();
        for (var entry : variables(booking).entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
    }

    public String documentRef(Booking booking) {
        var year = booking.getEvenementDatum() == null ? LocalDate.now().getYear() : booking.getEvenementDatum().getYear();
        var id = booking.getId() == null ? 0 : booking.getId();
        return "LUX-OFFERTE-" + year + "-" + String.format("%04d", id);
    }

    public OfferteResponse toResponse(Booking booking) {
        return new OfferteResponse(
                booking.getId(),
                booking.getStatus(),
                documentRef(booking),
                booking.getOfferteDatum(),
                booking.getOfferteSentDate(),
                booking.getOndertekeningDatum(),
                booking.getDocusealSubmissionId(),
                booking.getOffertePdfPath(),
                booking.getOffertePdfPath() == null ? null : "/api/bookings/" + booking.getId() + "/offerte/download"
        );
    }

    private Path savePdf(Booking booking, byte[] pdf) {
        try {
            Files.createDirectories(OFFERTE_DIR);
            var path = OFFERTE_DIR.resolve(booking.getId() + ".pdf").toAbsolutePath().normalize();
            Files.write(path, pdf);
            return path;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Offerte-PDF kon niet worden opgeslagen.");
        }
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
    }

    private Map<String, String> variables(Booking booking) {
        var offerteDatum = booking.getOfferteDatum() == null ? LocalDate.now() : booking.getOfferteDatum();
        var aanbetalingDatum = booking.getAanbetalingDeadline() == null
                ? offerteDatum.plusDays(7)
                : booking.getAanbetalingDeadline();
        var restantDatum = booking.getRestantDeadline() == null
                ? booking.getEvenementDatum().minusDays(14)
                : booking.getRestantDeadline();

        var variables = new LinkedHashMap<String, String>();
        variables.put("KLANT_NAAM", escape(booking.getCustomer().getNaam()));
        variables.put("EVENEMENT_DATUM", formatDate(booking.getEvenementDatum()));
        variables.put("EVENEMENT_SOORT", booking.getEvenementType().name().toLowerCase(NL));
        variables.put("SUBPRIJZEN_RIJEN", subPrijsRows(booking));
        variables.put("SUBTOTAAL", money(booking.getSubtotaal()));
        variables.put("KORTING", money(booking.getKorting()));
        variables.put("TOTAAL", money(booking.getTotaal()));
        variables.put("AANBETALING_BEDRAG", money(booking.getAanbetalingBedrag()));
        variables.put("AANBETALING_DAGEN", "7");
        variables.put("RESTANT_BEDRAG", money(booking.getRestantBedrag()));
        variables.put("GELDIG_TOT", formatDate(offerteDatum.plusDays(14)));
        variables.put("AANBETALING_DATUM", formatDate(aanbetalingDatum));
        variables.put("RESTANT_DATUM", formatDate(restantDatum));
        variables.put("AANTAL_GASTEN", String.valueOf(booking.getAantalGasten()));
        variables.put("EXTRA_EIGENSCHAPPEN", eigenschappen(booking));
        variables.put("BETAAL_OMSCHRIJVING", booking.getEvenementDatum().format(PAYMENT_REF_DATE) + " - " + documentRef(booking));
        variables.put("ONDERTEKENING_DATUM", booking.getOndertekeningDatum() == null ? "" : formatDate(booking.getOndertekeningDatum()));
        variables.put("DOCUMENT_REF", documentRef(booking));
        return variables;
    }

    private String subPrijsRows(Booking booking) {
        return booking.getSubPrijzen().stream()
                .map(this::subPrijsRow)
                .reduce("", String::concat);
    }

    private String subPrijsRow(SubPrijs subPrijs) {
        var bedrag = money(subPrijs.getBedrag());
        return """
                <tr>
                  <td>%s</td>
                  <td class="center">%s</td>
                  <td class="center">1</td>
                  <td class="right">%s</td>
                </tr>
                """.formatted(escape(subPrijs.getNaam()), bedrag, bedrag);
    }

    private String eigenschappen(Booking booking) {
        return booking.getEigenschappen().stream()
                .map(item -> "<li>" + escape(item) + "</li>")
                .reduce("", String::concat);
    }

    private String loadTemplate() {
        try {
            var resource = new ClassPathResource("templates/offerte-lux.html");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Offerte-template kon niet worden geladen.");
        }
    }

    private String toXhtml(String html) {
        return html
                .replace("<meta charset=\"UTF-8\">", "<meta charset=\"UTF-8\" />")
                .replace("<br>", "<br />")
                .replace("<br/>", "<br />")
                .replaceAll("<img([^>]*?)(?<!/)>", "<img$1 />")
                .replace("&nbsp;", "&#160;")
                .replace("&copy;", "&#169;");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(NL)
                .format(amount == null ? BigDecimal.ZERO : amount)
                .replace('\u00A0', ' ');
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
}
