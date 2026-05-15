package nl.partycentrum.lux.service;

import jakarta.mail.MessagingException;
import nl.partycentrum.lux.config.MailProperties;
import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.Invoice;
import nl.partycentrum.lux.domain.MailLogType;
import nl.partycentrum.lux.domain.PaymentPart;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final Locale NL = new Locale("nl", "NL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LOCATION = "Bennebroekerweg 530, 2132MD Hoofddorp";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final CompanySettingsService companySettingsService;
    private final MailLogService mailLogService;
    private final BookingRepository bookingRepository;

    public MailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            CompanySettingsService companySettingsService,
            MailLogService mailLogService,
            BookingRepository bookingRepository
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.companySettingsService = companySettingsService;
        this.mailLogService = mailLogService;
        this.bookingRepository = bookingRepository;
    }

    public void sendBookingConfirmation(Booking booking) {
        var customer = booking.getCustomer();
        var subject = "Bevestiging boeking Partycentrum Lux";
        var html = layout(
                "Boeking bevestigd",
                """
                        <p>Beste %s,</p>
                        <p>Van harte gefeliciteerd, uw boeking bij Partycentrum Lux is bevestigd. We kijken ernaar uit om samen een mooie dag neer te zetten.</p>
                        %s
                        %s
                        """.formatted(
                        customer.getName(),
                        summaryCard(
                                row("Datum", booking.getEventDate().format(DATE)),
                                row("Tijden", timeRange(booking.getStartTime(), booking.getEndTime())),
                                row("Totaalbedrag", money(booking.getTotaal())),
                                row("Aanbetaling deadline", deadlineOrText(booking.getAanbetalingDeadline(), "binnen 7 dagen na ondertekening"))
                        ),
                        darkButton("Vragen? Neem contact op", "mailto:" + companySettingsService.resolve().getEmail())
                )
        );
        sendHtml(customer.getEmail(), subject, html, MailLogType.BEVESTIGINGSMAIL, booking.getId(), null);
    }

    public void sendPaymentReminder(Invoice invoice) {
        var customer = invoice.getBooking().getCustomer();
        var html = layout(
                "Factuur vervalt binnenkort",
                """
                        <p>Beste %s,</p>
                        <p>Een vriendelijke herinnering: factuur <strong>%s</strong> vervalt binnenkort.</p>
                        <p><strong>Bedrag:</strong> %s<br><strong>Vervaldatum:</strong> %s</p>
                        <p>Wilt u de betaling uiterlijk op de vervaldatum voldoen? Alvast hartelijk dank.</p>
                        """.formatted(
                        customer.getName(),
                        invoice.getInvoiceNumber(),
                        money(invoice.getTotalAmount()),
                        invoice.getDueDate().format(DATE)
                )
        );
        sendHtml(customer.getEmail(), "Betalingsherinnering " + invoice.getInvoiceNumber(), html, reminderType(invoice), invoice.getBooking().getId(), null);
    }

    public void sendContractSigningRequest(Booking booking, String signingUrl) {
        var customer = booking.getCustomer();
        var html = layout(
                "Uw contract staat klaar",
                """
                        <p>Beste %s,</p>
                        <p>Het contract voor uw evenement bij Partycentrum Lux staat klaar om digitaal te ondertekenen.</p>
                        <p><strong>Evenement:</strong> %s<br><strong>Datum:</strong> %s<br><strong>Tijd:</strong> %s - %s</p>
                        <p>
                          <a href="%s" style="display:inline-block;background:#0f172a;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">
                            Contract bekijken en ondertekenen
                          </a>
                        </p>
                        <p>Werkt de knop niet? Kopieer dan deze link in uw browser:<br>%s</p>
                        """.formatted(
                        customer.getName(),
                        booking.getEventType().name().toLowerCase(),
                        booking.getEventDate().format(DATE),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        signingUrl,
                        signingUrl
                )
        );
        sendHtml(customer.getEmail(), "Uw contract - Partycentrum Lux", html, MailLogType.OFFERTE_VERZONDEN, booking.getId(), null);
    }

    public void sendContractSignedConfirmation(Booking booking) {
        var customer = booking.getCustomer();
        var html = layout(
                "Contract ondertekend",
                """
                        <p>Beste %s,</p>
                        <p>Van harte gefeliciteerd, het contract is succesvol ondertekend. De boeking is nu definitief bevestigd.</p>
                        %s
                        %s
                        """.formatted(
                        customer.getName(),
                        summaryCard(
                                row("Datum", booking.getEventDate().format(DATE)),
                                row("Tijden", timeRange(booking.getStartTime(), booking.getEndTime())),
                                row("Totaalbedrag", money(booking.getTotaal())),
                                row("Aanbetaling deadline", deadlineOrText(booking.getAanbetalingDeadline(), "binnen 7 dagen na ondertekening"))
                        ),
                        darkButton("Vragen? Neem contact op", "mailto:" + companySettingsService.resolve().getEmail())
                )
        );
        sendHtml(customer.getEmail(), "Contract ondertekend - Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, booking.getId(), null);
    }

    public void sendOfferteSignedConfirmation(Booking booking) {
        var customer = booking.getCustomer();
        var settings = companySettingsService.resolve();
        var html = layout(
                "Offerte ondertekend",
                """
                        <p>Beste %s,</p>
                        <p>Van harte gefeliciteerd, uw offerte is digitaal ondertekend. Daarmee is uw boeking bij Partycentrum Lux officieel bevestigd.</p>
                        %s
                        <p>De aanbetaling kan worden overgemaakt naar <strong>%s</strong> onder vermelding van uw naam en evenementdatum.</p>
                        %s
                        """.formatted(
                        customer.getNaam(),
                        summaryCard(
                                row("Datum", booking.getEventDate().format(DATE)),
                                row("Tijden", timeRange(booking.getStartTime(), booking.getEndTime())),
                                row("Totaalbedrag", money(booking.getTotaal())),
                                row("Aanbetaling deadline", deadlineOrText(booking.getAanbetalingDeadline(), "binnen 7 dagen na ondertekening"))
                        ),
                        settings.getIban(),
                        darkButton("Vragen? Neem contact op", "mailto:" + settings.getEmail())
                )
        );
        sendHtml(customer.getEmail(), "Offerte ondertekend - Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, booking.getId(), null);
    }

    public void sendAanbetalingReminder(Booking booking) {
        var customer = booking.getCustomer();
        var settings = companySettingsService.resolve();
        var html = layout(
                "Betalingsherinnering aanbetaling",
                """
                        <p>Beste %s,</p>
                        <p>Een vriendelijke herinnering voor de aanbetaling van uw boeking bij Partycentrum Lux.</p>
                        %s
                        <p>Alvast hartelijk dank voor de betaling. Heeft u vragen, dan helpen wij u graag.</p>
                        """.formatted(
                        customer.getNaam(),
                        goldBox(
                                row("Bedrag", money(booking.getAanbetalingBedrag())),
                                row("IBAN", settings.getIban()),
                                row("Deadline", deadlineOrText(booking.getAanbetalingDeadline(), "-"))
                        )
                )
        );
        sendHtml(customer.getEmail(), "Betalingsherinnering aanbetaling - Partycentrum Lux", html, MailLogType.BETALING_HERINNERING_AANBETALING, booking.getId(), null);
    }

    public void sendRestantReminder(Booking booking) {
        var customer = booking.getCustomer();
        var settings = companySettingsService.resolve();
        var html = layout(
                "Betalingsherinnering restant",
                """
                        <p>Beste %s,</p>
                        <p>Wij hebben uw aanbetaling ontvangen, dank daarvoor. Dit is een vriendelijke herinnering voor het resterende bedrag.</p>
                        %s
                        <p>Na ontvangst staat de betaling volledig verwerkt in uw dossier.</p>
                        """.formatted(
                        customer.getNaam(),
                        goldBox(
                                row("Restant bedrag", money(booking.getRestantBedrag())),
                                row("IBAN", settings.getIban()),
                                row("Deadline", deadlineOrText(booking.getRestantDeadline(), "-"))
                        )
                )
        );
        sendHtml(customer.getEmail(), "Betalingsherinnering restant - Partycentrum Lux", html, MailLogType.BETALING_HERINNERING_RESTANT, booking.getId(), null);
    }

    public void sendPaymentConfirmation(Booking booking, PaymentPart part) {
        var customer = booking.getCustomer();
        var amount = part == PaymentPart.AANBETALING ? booking.getAanbetalingBedrag() : booking.getRestantBedrag();
        var paidDate = part == PaymentPart.AANBETALING ? booking.getAanbetalingBetaaldDatum() : booking.getRestantBetaaldDatum();
        var label = part == PaymentPart.AANBETALING ? "aanbetaling" : "restantbetaling";
        var html = layout(
                "Betaling ontvangen",
                """
                        <p>Beste %s,</p>
                        <p>Hartelijk dank. Wij hebben uw %s ontvangen.</p>
                        <p><strong>Bedrag:</strong> %s<br><strong>Betaaldatum:</strong> %s</p>
                        """.formatted(
                        customer.getNaam(),
                        label,
                        money(amount),
                        paidDate == null ? LocalDate.now().format(DATE) : paidDate.format(DATE)
                )
        );
        sendHtml(customer.getEmail(), "Betaling ontvangen - Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, booking.getId(), null);
    }

    public void sendInvoice(Invoice invoice) {
        var customer = invoice.getBooking().getCustomer();
        var html = layout(
                "Uw factuur",
                """
                        <p>Beste %s,</p>
                        <p>In de bijlage vindt u factuur <strong>%s</strong> voor uw evenement bij Partycentrum Lux.</p>
                        <p><strong>Bedrag:</strong> %s<br><strong>Vervaldatum:</strong> %s</p>
                        """.formatted(
                        customer.getName(),
                        invoice.getInvoiceNumber(),
                        money(invoice.getTotalAmount()),
                        invoice.getDueDate().format(DATE)
                )
        );
        sendHtmlWithAttachment(customer.getEmail(), "Factuur " + invoice.getInvoiceNumber() + " - Partycentrum Lux", html, invoice, MailLogType.BEVESTIGINGSMAIL, invoice.getBooking().getId(), null);
    }

    public void sendPaymentConfirmation(Invoice invoice) {
        var customer = invoice.getBooking().getCustomer();
        var html = layout(
                "Betaling ontvangen",
                """
                        <p>Beste %s,</p>
                        <p>Hartelijk dank. Wij hebben de betaling voor factuur <strong>%s</strong> ontvangen.</p>
                        <p><strong>Betaald bedrag:</strong> %s<br><strong>Betaaldatum:</strong> %s</p>
                        """.formatted(
                        customer.getName(),
                        invoice.getInvoiceNumber(),
                        money(invoice.getTotalAmount()),
                        invoice.getPaidDate().format(DATE)
                )
        );
        sendHtml(customer.getEmail(), "Betaling ontvangen - Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, invoice.getBooking().getId(), null);
    }

    public void sendBookingFullyPaid(Booking booking) {
        var customer = booking.getCustomer();
        var html = layout(
                "Boeking volledig betaald",
                """
                        <p>Beste %s,</p>
                        <p>Uw boeking voor <strong>%s</strong> is volledig betaald. Bedankt voor de snelle afhandeling.</p>
                        <p>We kijken ernaar uit u te ontvangen bij Partycentrum Lux.</p>
                        """.formatted(customer.getName(), booking.getEventDate().format(DATE))
        );
        sendHtml(customer.getEmail(), "Boeking volledig betaald - Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, booking.getId(), null);
    }

    public void sendEventReminder(Booking booking) {
        var customer = booking.getCustomer();
        var settings = companySettingsService.resolve();
        var html = layout(
                "Uw evenement komt eraan",
                """
                        <p>Beste %s,</p>
                        <p>Over 7 dagen is het zover. We kijken ernaar uit om u en uw gasten te ontvangen bij Partycentrum Lux.</p>
                        %s
                        <h2>Algemene regels</h2>
                        <p>%s</p>
                        <p>We kijken ernaar uit u binnenkort te verwelkomen.</p>
                        """.formatted(
                        customer.getName(),
                        summaryCard(
                                row("Datum", booking.getEventDate().format(DATE)),
                                row("Tijden", timeRange(booking.getStartTime(), booking.getEndTime())),
                                row("Locatie", LOCATION),
                                row("Aantal gasten", String.valueOf(booking.getGuestCount()))
                        ),
                        textBlock(settings.getGeneralTerms())
                )
        );
        sendHtml(customer.getEmail(), "Uw evenement is over 7 dagen - Partycentrum Lux", html, MailLogType.EVENEMENT_HERINNERING, booking.getId(), null);
    }

    public void sendThankYou(Booking booking) {
        sendReviewRequest(booking);
    }

    public void sendReviewRequest(Booking booking) {
        var customer = booking.getCustomer();
        var settings = companySettingsService.resolve();
        var html = layout(
                "Bedankt voor uw bezoek",
                """
                        <p>Beste %s,</p>
                        <p>Hartelijk dank dat u uw evenement bij Partycentrum Lux heeft gevierd. We hopen dat u een prachtige dag heeft gehad.</p>
                        <p>Wilt u uw ervaring met ons delen? Een korte Google review betekent veel voor ons en helpt nieuwe gasten bij hun keuze.</p>
                        %s
                        """.formatted(customer.getNaam(), goldButton("Laat een Google review achter", settings.getGoogleReviewUrl()))
        );
        sendHtml(customer.getEmail(), "Bedankt voor uw evenement - Partycentrum Lux", html, MailLogType.REVIEW_VERZOEK, booking.getId(), null);
    }

    public void sendCancellation(Booking booking) {
        var customer = booking.getCustomer();
        var html = layout(
                "Boeking geannuleerd",
                """
                        <p>Beste %s,</p>
                        <p>Hierbij bevestigen wij dat uw boeking voor <strong>%s</strong> is geannuleerd. We begrijpen dat plannen kunnen veranderen.</p>
                        %s
                        <p>Heeft u vragen over deze annulering, neem dan gerust contact met ons op.</p>
                        %s
                        """.formatted(
                        customer.getName(),
                        booking.getEventDate().format(DATE),
                        goldBox(row("Reden", textBlock(booking.getAnnuleringsReden() == null || booking.getAnnuleringsReden().isBlank()
                                ? "Geen reden opgegeven."
                                : booking.getAnnuleringsReden()))),
                        darkButton("Contact opnemen", "mailto:" + companySettingsService.resolve().getEmail())
                )
        );
        sendHtml(customer.getEmail(), "Boeking geannuleerd - Partycentrum Lux", html, MailLogType.ANNULERING, booking.getId(), null);
    }

    public void sendBezichtigingConfirmation(
            Long bezichtigingId,
            String klantNaam,
            String klantEmail,
            LocalDate datum,
            LocalTime startTijd,
            LocalTime eindTijd,
            byte[] icsFile
    ) {
        var html = layout(
                "Bezichtiging bevestigd",
                """
                        <p>Beste %s,</p>
                        <p>Uw bezichtiging bij Partycentrum Lux is bevestigd. We ontvangen u graag op onderstaande afspraak.</p>
                        %s
                        <p>De agenda-uitnodiging is als .ics bestand toegevoegd aan deze e-mail.</p>
                        %s
                        """.formatted(
                        klantNaam,
                        summaryCard(
                                row("Datum", datum.format(DATE)),
                                row("Tijden", timeRange(startTijd, eindTijd)),
                                row("Locatie", LOCATION),
                                row("Contactpersoon", "Hussain Siddiqui - 0647107251")
                        ),
                        darkButton("Vragen? Neem contact op", "mailto:" + companySettingsService.resolve().getEmail())
                )
        );
        sendHtmlWithBytesAttachment(
                klantEmail,
                "Bezichtiging bevestigd - Partycentrum Lux",
                html,
                "bezichtiging-partycentrum-lux.ics",
                icsFile,
                "text/calendar",
                MailLogType.BEZICHTIGING_BEVESTIGING,
                null,
                bezichtigingId
        );
    }

    public void sendBezichtigingReminder(
            Long bezichtigingId,
            String klantNaam,
            String klantEmail,
            LocalDate datum,
            LocalTime startTijd,
            LocalTime eindTijd
    ) {
        var html = layout(
                "Herinnering bezichtiging",
                """
                        <p>Beste %s,</p>
                        <p>Een vriendelijke herinnering: morgen staat uw bezichtiging bij Partycentrum Lux gepland.</p>
                        %s
                        <h2>Parkeren</h2>
                        <p>U kunt gratis parkeren direct bij de locatie. Meld u bij aankomst bij Hussain.</p>
                        %s
                        """.formatted(
                        klantNaam,
                        summaryCard(
                                row("Datum", datum.format(DATE)),
                                row("Tijden", timeRange(startTijd, eindTijd)),
                                row("Locatie", LOCATION),
                                row("Contactpersoon", "Hussain Siddiqui - 0647107251")
                        ),
                        darkButton("Vragen? Neem contact op", "mailto:" + companySettingsService.resolve().getEmail())
                )
        );
        sendHtml(klantEmail, "Herinnering: morgen bezichtiging Partycentrum Lux", html, MailLogType.BEZICHTIGING_HERINNERING, null, bezichtigingId);
    }

    public void sendWeeklyOwnerSummary(List<Booking> bookings, BigDecimal expectedRevenue, BigDecimal outstandingPayments) {
        var rows = bookings.stream()
                .map(booking -> """
                        <tr>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                        </tr>
                        """.formatted(
                        booking.getEventDate().format(DATE),
                        booking.getCustomer().getName(),
                        booking.getEventType(),
                        money(booking.getPrice())
                ))
                .reduce("", String::concat);

        var html = layout(
                "Weekoverzicht Partycentrum Lux",
                """
                        <p>Goedemorgen,</p>
                        <p>Dit staat er deze week op de planning.</p>
                        <table>
                          <thead><tr><th>Datum</th><th>Klant</th><th>Type</th><th>Omzet</th></tr></thead>
                          <tbody>%s</tbody>
                        </table>
                        <p><strong>Verwachte omzet:</strong> %s<br><strong>Openstaande betalingen:</strong> %s</p>
                        """.formatted(rows.isBlank() ? "<tr><td colspan=\"4\">Geen boekingen deze week.</td></tr>" : rows,
                        money(expectedRevenue),
                        money(outstandingPayments))
        );
        sendHtml(mailProperties.ownerEmail(), "Weekoverzicht Partycentrum Lux", html, MailLogType.BEVESTIGINGSMAIL, null, null);
    }

    public void sendOfferteSignedConfirmationByBookingId(Long bookingId) {
        sendOfferteSignedConfirmation(getBooking(bookingId));
    }

    public void sendAanbetalingReminderByBookingId(Long bookingId) {
        sendAanbetalingReminder(getBooking(bookingId));
    }

    public void sendRestantReminderByBookingId(Long bookingId) {
        sendRestantReminder(getBooking(bookingId));
    }

    public void sendEventReminderByBookingId(Long bookingId) {
        sendEventReminder(getBooking(bookingId));
    }

    public void sendReviewRequestByBookingId(Long bookingId) {
        sendReviewRequest(getBooking(bookingId));
    }

    public void sendCancellationByBookingId(Long bookingId) {
        sendCancellation(getBooking(bookingId));
    }

    private void sendHtml(String to, String subject, String html, MailLogType type, Long bookingId, Long bezichtigingId) {
        if (!mailProperties.enabled()) {
            log.info("Mail disabled. Would send '{}' to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
            return;
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(companySettingsService.resolve().getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Mail sent '{}' to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
        } catch (MessagingException | MailException exception) {
            mailLogService.logFailed(bookingId, bezichtigingId, type, to, subject, exception);
            throw new IllegalStateException("E-mail kon niet worden verzonden.", exception);
        }
    }

    private void sendHtmlWithAttachment(
            String to,
            String subject,
            String html,
            Invoice invoice,
            MailLogType type,
            Long bookingId,
            Long bezichtigingId
    ) {
        if (!mailProperties.enabled()) {
            log.info("Mail disabled. Would send '{}' with invoice attachment to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
            return;
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(companySettingsService.resolve().getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (invoice.getPdfPath() != null) {
                helper.addAttachment(invoice.getInvoiceNumber() + ".pdf", new FileSystemResource(Path.of(invoice.getPdfPath())));
            }
            mailSender.send(message);
            log.info("Mail sent '{}' with invoice attachment to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
        } catch (MessagingException | MailException exception) {
            mailLogService.logFailed(bookingId, bezichtigingId, type, to, subject, exception);
            throw new IllegalStateException("E-mail kon niet worden verzonden.", exception);
        }
    }

    private void sendHtmlWithBytesAttachment(
            String to,
            String subject,
            String html,
            String filename,
            byte[] attachment,
            String contentType,
            MailLogType type,
            Long bookingId,
            Long bezichtigingId
    ) {
        if (!mailProperties.enabled()) {
            log.info("Mail disabled. Would send '{}' with attachment to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
            return;
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(companySettingsService.resolve().getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (attachment != null && attachment.length > 0) {
                helper.addAttachment(filename, new ByteArrayResource(attachment), contentType);
            }
            mailSender.send(message);
            log.info("Mail sent '{}' with attachment to {}", subject, to);
            mailLogService.logSent(bookingId, bezichtigingId, type, to, subject);
        } catch (MessagingException | MailException exception) {
            mailLogService.logFailed(bookingId, bezichtigingId, type, to, subject, exception);
            throw new IllegalStateException("E-mail kon niet worden verzonden.", exception);
        }
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
    }

    private MailLogType reminderType(Invoice invoice) {
        return switch (invoice.getInvoiceType()) {
            case AANBETALING -> MailLogType.BETALING_HERINNERING_AANBETALING;
            case RESTANT -> MailLogType.BETALING_HERINNERING_RESTANT;
            default -> MailLogType.BEVESTIGINGSMAIL;
        };
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td>%s</td>
                  <td>%s</td>
                </tr>
                """.formatted(label, value == null || value.isBlank() ? "-" : value);
    }

    private String summaryCard(String... rows) {
        return """
                <div class="summary-card">
                  <table role="presentation">%s</table>
                </div>
                """.formatted(String.join("", rows));
    }

    private String goldBox(String... rows) {
        return """
                <div class="gold-box">
                  <table role="presentation">%s</table>
                </div>
                """.formatted(String.join("", rows));
    }

    private String darkButton(String label, String href) {
        return button(label, href, "button");
    }

    private String goldButton(String label, String href) {
        return button(label, href, "button gold-button");
    }

    private String button(String label, String href, String cssClass) {
        if (href == null || href.isBlank()) {
            return "";
        }
        return """
                <p class="button-row"><a class="%s" href="%s">%s</a></p>
                """.formatted(cssClass, href, label);
    }

    private String deadlineOrText(LocalDate date, String fallback) {
        return date == null ? fallback : date.format(DATE);
    }

    private String timeRange(LocalTime start, LocalTime end) {
        return (start == null ? "-" : start.format(TIME)) + " - " + (end == null ? "-" : end.format(TIME));
    }

    private String layout(String title, String body) {
        var settings = companySettingsService.resolve();
        return """
                <!doctype html>
                <html lang="nl">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin:0; background:#ffffff; font-family:Arial,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif; color:#1a1a1a; font-size:15px; line-height:1.7; }
                    .wrap { max-width:600px; margin:0 auto; background:#ffffff; }
                    .header { background:#1a1a1a; padding:30px 40px 26px; color:#ffffff; }
                    .brand { margin:0; color:#DCAB46; font-size:22px; line-height:1.2; font-weight:700; }
                    .title { margin:10px 0 0; color:#ffffff; font-size:18px; line-height:1.4; font-weight:600; }
                    .divider { height:2px; background:#DCAB46; }
                    .body { padding:40px; background:#ffffff; }
                    .body p { margin:0 0 18px; }
                    h2 { margin:26px 0 10px; font-size:16px; line-height:1.4; color:#1a1a1a; }
                    table { width:100%%; border-collapse:collapse; }
                    td { padding:7px 0; vertical-align:top; border-bottom:1px solid #eadcb8; }
                    td:first-child { color:#666666; width:44%%; }
                    td:last-child { color:#1a1a1a; font-weight:700; text-align:right; }
                    .summary-card { margin:24px 0; border:1px solid #DCAB46; background:#fffaf0; padding:18px 20px; }
                    .gold-box { margin:24px 0; border-left:4px solid #DCAB46; background:#fff7e6; padding:18px 20px; }
                    .button-row { margin:26px 0 0; }
                    .button { display:inline-block; background:#1a1a1a; color:#ffffff !important; text-decoration:none; padding:12px 18px; border-radius:6px; font-weight:700; }
                    .gold-button { background:#DCAB46; color:#1a1a1a !important; }
                    .footer { background:#F5F5F5; color:#666666; padding:24px 40px; font-size:13px; line-height:1.6; }
                    .footer strong { color:#1a1a1a; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="header">
                      <h1 class="brand">%s</h1>
                      <p class="title">%s</p>
                    </div>
                    <div class="divider"></div>
                    <div class="body">%s</div>
                    <div class="footer">
                      <strong>%s</strong><br>
                      %s<br>
                      %s %s<br>
                      %s<br>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                settings.getCompanyName(),
                title,
                body,
                settings.getCompanyName(),
                settings.getAddress(),
                settings.getPostalCode(),
                settings.getCity(),
                settings.getPhone(),
                settings.getEmail()
        );
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(NL).format(amount);
    }

    private String textBlock(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
