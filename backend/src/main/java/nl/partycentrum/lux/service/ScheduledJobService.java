package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduledJobService {

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;

    public ScheduledJobService(
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            MailService mailService
    ) {
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.mailService = mailService;
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Amsterdam")
    public void runDailyJobs() {
        var today = LocalDate.now();
        sendPaymentReminders(today);
        sendEventReminders(today);
        autoFinishFullyPaidBookings(today);
        sendReviewRequests(today);
        invoiceService.markOverdueInvoices();
    }

    @Transactional
    public void sendPaymentReminders(LocalDate today) {
        invoiceRepository.findByStatusAndDueDate(InvoiceStatus.ONBETAALD, today.plusDays(3))
                .forEach(mailService::sendPaymentReminder);
    }

    @Transactional
    public void sendEventReminders(LocalDate today) {
        bookingRepository.findByEventDateAndStatusIn(
                        today.plusDays(7),
                        List.of(
                                BookingStatus.FACTUUR_VERZONDEN,
                                BookingStatus.AANBETALING_BETAALD,
                                BookingStatus.VOLLEDIG_BETAALD
                        )
                )
                .forEach(mailService::sendEventReminder);
    }

    @Transactional
    public void sendReviewRequests(LocalDate today) {
        bookingRepository.findByEventDateAndStatus(today.minusDays(1), BookingStatus.AFGEROND)
                .forEach(mailService::sendReviewRequest);
    }

    @Transactional
    public void autoFinishFullyPaidBookings(LocalDate today) {
        bookingRepository.findByStatusAndEventDateBefore(BookingStatus.VOLLEDIG_BETAALD, today)
                .forEach(booking -> booking.setStatus(BookingStatus.AFGEROND));
    }
}
