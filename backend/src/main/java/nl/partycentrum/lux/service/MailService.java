package nl.partycentrum.lux.service;

import jakarta.mail.MessagingException;
import nl.partycentrum.lux.config.MailProperties;
import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.Invoice;
import nl.partycentrum.lux.domain.MailLogType;
import nl.partycentrum.lux.domain.PaymentPart;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final Locale NL = new Locale("nl", "NL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
                "Uw boeking is bevestigd",
                """
                        <p>Beste %s,</p>
                        <p>Wat fijn dat u voor Partycentrum Lux kiest. Uw %s op <strong>%s</strong> is bevestigd.</p>
                        <p><strong>Aantal gasten:</strong> %s<br><strong>Totaalprijs:</strong> %s</p>
                        <p>Heeft u nog vragen of wilt u details aanpassen? Reageer gerust op deze e-mail.</p>
                        """.formatted(
                        customer.getName(),
                        booking.getEventType().name().toLowerCase(),
                        booking.getEventDate().format(DATE),
                        booking.getGuestCount(),
                        money(booking.getPrice())
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
                        <p>Het contract voor uw evenement op <strong>%s</strong> is succesvol ondertekend.</p>
                        <p>De boeking is nu bevestigd. Wij maken de factuur gereed en sturen deze apart toe.</p>
                        <p><strong>Evenement:</strong> %s<br><strong>Aantal gasten:</strong> %s</p>
                        """.formatted(
                        customer.getName(),
                        booking.getEventDate().format(DATE),
                        booking.getEventType(),
                        booking.getGuestCount()
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
                        <p>Uw offerte voor <strong>%s</strong> is digitaal ondertekend. Daarmee is de boeking bevestigd.</p>
                        <p><strong>Datum:</strong> %s<br><strong>Tijd:</strong> %s - %s<br><strong>Totaalbedrag:</strong> %s</p>
                        <p><strong>Aanbetaling:</strong> %s uiterlijk %s<br><strong>IBAN:</strong> %s</p>
                        """.formatted(
                        customer.getNaam(),
                        booking.getEventType().name().toLowerCase(),
                        booking.getEventDate().format(DATE),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        money(booking.getTotaal()),
                        money(booking.getAanbetalingBedrag()),
                        booking.getAanbetalingDeadline() == null ? "binnen 7 dagen na ondertekening" : booking.getAanbetalingDeadline().format(DATE),
                        settings.getIban()
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
                        <p><strong>Bedrag:</strong> %s<br><strong>Deadline:</strong> %s<br><strong>IBAN:</strong> %s</p>
                        """.formatted(
                        customer.getNaam(),
                        money(booking.getAanbetalingBedrag()),
                        booking.getAanbetalingDeadline() == null ? "-" : booking.getAanbetalingDeadline().format(DATE),
                        settings.getIban()
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
                        <p>Een vriendelijke herinnering voor het resterende bedrag van uw boeking.</p>
                        <p><strong>Bedrag:</strong> %s<br><strong>Deadline:</strong> %s<br><strong>IBAN:</strong> %s</p>
                        """.formatted(
                        customer.getNaam(),
                        money(booking.getRestantBedrag()),
                        booking.getRestantDeadline() == null ? "-" : booking.getRestantDeadline().format(DATE),
                        settings.getIban()
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
                        <p>Over 7 dagen ontvangen wij u voor uw evenement bij Partycentrum Lux.</p>
                        <p><strong>Datum:</strong> %s<br><strong>Tijd:</strong> %s - %s<br><strong>Aantal gasten:</strong> %s</p>
                        <p><strong>Voorwaarden:</strong><br>%s</p>
                        """.formatted(
                        customer.getName(),
                        booking.getEventDate().format(DATE),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getGuestCount(),
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
                        <p>Bedankt dat u uw evenement bij Partycentrum Lux heeft gevierd. We hopen dat u een prachtige dag heeft gehad.</p>
                        <p>Wilt u uw ervaring met ons delen? Uw review helpt nieuwe gasten bij het kiezen van onze locatie.</p>
                        <p><a href="%s" style="color:#0f172a;font-weight:700;">Laat een Google review achter</a></p>
                        """.formatted(customer.getNaam(), settings.getGoogleReviewUrl())
        );
        sendHtml(customer.getEmail(), "Bedankt voor uw evenement - Partycentrum Lux", html, MailLogType.REVIEW_VERZOEK, booking.getId(), null);
    }

    public void sendCancellation(Booking booking) {
        var customer = booking.getCustomer();
        var html = layout(
                "Boeking geannuleerd",
                """
                        <p>Beste %s,</p>
                        <p>Uw boeking voor <strong>%s</strong> is geannuleerd.</p>
                        <p><strong>Reden:</strong><br>%s</p>
                        <p>Eventuele openstaande niet-aanbetalingsfacturen zijn ingetrokken.</p>
                        """.formatted(
                        customer.getName(),
                        booking.getEventDate().format(DATE),
                        textBlock(booking.getAnnuleringsReden() == null || booking.getAnnuleringsReden().isBlank()
                                ? "Geen reden opgegeven."
                                : booking.getAnnuleringsReden())
                )
        );
        sendHtml(customer.getEmail(), "Boeking geannuleerd - Partycentrum Lux", html, MailLogType.ANNULERING, booking.getId(), null);
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

    private String layout(String title, String body) {
        var settings = companySettingsService.resolve();
        return """
                <!doctype html>
                <html lang="nl">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin:0; background:#f6f8fb; font-family:Inter,Arial,sans-serif; color:#172033; }
                    .wrap { max-width:680px; margin:0 auto; padding:32px 18px; }
                    .card { background:#ffffff; border:1px solid #e5eaf1; border-radius:16px; overflow:hidden; }
                    .head { background:#0f172a; color:#ffffff; padding:28px 32px; }
                    .body { padding:32px; line-height:1.7; }
                    h1 { margin:0; font-size:24px; }
                    table { width:100%%; border-collapse:collapse; margin:18px 0; }
                    th, td { border-bottom:1px solid #e5eaf1; padding:10px; text-align:left; }
                    .foot { color:#667085; font-size:13px; padding:0 32px 28px; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="head"><h1>%s</h1><p>%s</p></div>
                      <div class="body">%s</div>
                      <div class="foot">%s</div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(title, settings.getCompanyName(), body, settings.getAddress());
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
