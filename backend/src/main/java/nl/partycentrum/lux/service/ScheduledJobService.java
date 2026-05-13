package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ScheduledJobService {

    private final BookingRepository bookingRepository;
    private final MailService mailService;

    public ScheduledJobService(
            BookingRepository bookingRepository,
            MailService mailService
    ) {
        this.bookingRepository = bookingRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void runDailyJobs() {
        var today = LocalDate.now();
        sendPaymentReminders(today);
        sendEventReminders(today);
        autoFinishFullyPaidBookings(today);
    }

    @Transactional
    public void sendPaymentReminders(LocalDate today) {
        sendAanbetalingReminders(today);
        sendRestantReminders(today);
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Amsterdam")
    public void scheduledAanbetalingReminders() {
        sendAanbetalingReminders(LocalDate.now());
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Amsterdam")
    public void scheduledRestantReminders() {
        sendRestantReminders(LocalDate.now());
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Amsterdam")
    public void scheduledEventReminders() {
        sendEventReminders(LocalDate.now());
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Amsterdam")
    public void scheduledAutoFinishFullyPaidBookings() {
        autoFinishFullyPaidBookings(LocalDate.now());
    }

    @Transactional
    public void sendAanbetalingReminders(LocalDate today) {
        var reminderDate = today.plusDays(3);
        bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.BEVESTIGD)
                .filter(booking -> !booking.isAanbetalingBetaald())
                .filter(booking -> reminderDate.equals(booking.getAanbetalingDeadline()))
                .forEach(mailService::sendAanbetalingReminder);
    }

    @Transactional
    public void sendRestantReminders(LocalDate today) {
        var reminderDate = today.plusDays(3);
        bookingRepository.findAll().stream()
                .filter(booking -> booking.isAanbetalingBetaald())
                .filter(booking -> !booking.isRestantBetaald())
                .filter(booking -> reminderDate.equals(booking.getRestantDeadline()))
                .forEach(mailService::sendRestantReminder);
    }

    @Transactional
    public void sendEventReminders(LocalDate today) {
        bookingRepository.findByEventDateAndStatus(today.plusDays(7), BookingStatus.VOLLEDIG_BETAALD)
                .forEach(mailService::sendEventReminder);
    }

    @Transactional
    public void sendReviewRequests(LocalDate today) {
        // Review requests are intentionally sent immediately when the restantbetaling is marked as paid.
    }

    @Transactional
    public void autoFinishFullyPaidBookings(LocalDate today) {
        bookingRepository.findByStatusAndEventDateBefore(BookingStatus.VOLLEDIG_BETAALD, today)
                .forEach(booking -> booking.setStatus(BookingStatus.AFGEROND));
    }
}
