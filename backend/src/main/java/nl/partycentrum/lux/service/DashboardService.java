package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.dashboard.ChartPoint;
import nl.partycentrum.lux.dto.dashboard.DashboardStatsResponse;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private static final Locale NL = new Locale("nl", "NL");

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public DashboardService(
            BookingRepository bookingRepository,
            BookingService bookingService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        var currentMonth = YearMonth.now();
        var start = currentMonth.atDay(1);
        var end = currentMonth.atEndOfMonth();

        var bookings = bookingRepository.findAll();

        var revenueThisMonth = bookings.stream()
                .map(booking -> paidRevenueBetween(booking, start, end))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var outstandingPayments = bookings.stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CONCEPT)
                .filter(booking -> booking.getStatus() != BookingStatus.OFFERTE_VERZONDEN)
                .filter(booking -> booking.getStatus() != BookingStatus.GEANNULEERD)
                .map(this::outstandingForBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var revenue = new ArrayList<ChartPoint>();
        var bookingCounts = new ArrayList<ChartPoint>();
        for (int index = 11; index >= 0; index--) {
            var month = currentMonth.minusMonths(index);
            var monthStart = month.atDay(1);
            var monthEnd = month.atEndOfMonth();
            var label = month.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, NL) + " " + month.getYear();

            var monthlyRevenue = bookings.stream()
                    .map(booking -> paidRevenueBetween(booking, monthStart, monthEnd))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var monthlyBookings = bookings.stream()
                    .filter(booking -> !booking.getEventDate().isBefore(monthStart) && !booking.getEventDate().isAfter(monthEnd))
                    .count();

            revenue.add(new ChartPoint(label, monthlyRevenue, 0));
            bookingCounts.add(new ChartPoint(label, BigDecimal.ZERO, monthlyBookings));
        }

        var upcoming = bookingRepository.findTop5ByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAsc(LocalDate.now(), BookingStatus.GEANNULEERD)
                .stream()
                .map(bookingService::toResponse)
                .toList();
        return new DashboardStatsResponse(
                revenueThisMonth,
                bookingRepository.countByEventDateBetween(start, end),
                outstandingPayments,
                occupancyRate(start, end),
                revenue,
                bookingCounts,
                upcoming,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> upcomingBookings() {
        return bookingRepository.findTop5ByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAsc(LocalDate.now(), BookingStatus.GEANNULEERD)
                .stream()
                .map(bookingService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> recentInvoices() {
        return List.of();
    }

    private BigDecimal paidRevenueBetween(nl.partycentrum.lux.domain.Booking booking, LocalDate start, LocalDate end) {
        var amount = BigDecimal.ZERO;
        if (booking.isAanbetalingBetaald()
                && booking.getAanbetalingBetaaldDatum() != null
                && !booking.getAanbetalingBetaaldDatum().isBefore(start)
                && !booking.getAanbetalingBetaaldDatum().isAfter(end)) {
            amount = amount.add(booking.getAanbetalingBedrag());
        }
        if (booking.isRestantBetaald()
                && booking.getRestantBetaaldDatum() != null
                && !booking.getRestantBetaaldDatum().isBefore(start)
                && !booking.getRestantBetaaldDatum().isAfter(end)) {
            amount = amount.add(booking.getRestantBedrag());
        }
        return amount;
    }

    private BigDecimal outstandingForBooking(nl.partycentrum.lux.domain.Booking booking) {
        var amount = BigDecimal.ZERO;
        if (!booking.isAanbetalingBetaald()) {
            amount = amount.add(booking.getAanbetalingBedrag());
        }
        if (!booking.isRestantBetaald()) {
            amount = amount.add(booking.getRestantBedrag());
        }
        return amount;
    }

    private double occupancyRate(LocalDate start, LocalDate end) {
        var occupiedDays = bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.GEANNULEERD)
                .mapToLong(booking -> {
                    var date = booking.getEventDate();
                    if (date.isBefore(start) || date.isAfter(end)) {
                        return 0;
                    }
                    return 1;
                })
                .sum();
        var totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        return Math.min(100.0, (occupiedDays * 100.0) / totalDays);
    }
}
