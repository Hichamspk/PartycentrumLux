package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.dashboard.ChartPoint;
import nl.partycentrum.lux.dto.dashboard.DashboardStatsResponse;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.InvoiceRepository;
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
    private final InvoiceRepository invoiceRepository;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    public DashboardService(
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            BookingService bookingService,
            InvoiceService invoiceService
    ) {
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        var currentMonth = YearMonth.now();
        var start = currentMonth.atDay(1);
        var end = currentMonth.atEndOfMonth();

        var invoices = invoiceRepository.findAll();
        var bookings = bookingRepository.findAll();

        var revenueThisMonth = invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.BETAALD)
                .filter(invoice -> invoice.getPaidDate() != null)
                .filter(invoice -> !invoice.getPaidDate().isBefore(start) && !invoice.getPaidDate().isAfter(end))
                .map(invoice -> invoice.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var outstandingPayments = invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.ONBETAALD || invoice.getStatus() == InvoiceStatus.VERLOPEN)
                .map(invoice -> invoice.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var revenue = new ArrayList<ChartPoint>();
        var bookingCounts = new ArrayList<ChartPoint>();
        for (int index = 11; index >= 0; index--) {
            var month = currentMonth.minusMonths(index);
            var monthStart = month.atDay(1);
            var monthEnd = month.atEndOfMonth();
            var label = month.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, NL) + " " + month.getYear();

            var monthlyRevenue = invoices.stream()
                    .filter(invoice -> invoice.getStatus() == InvoiceStatus.BETAALD)
                    .filter(invoice -> invoice.getPaidDate() != null)
                    .filter(invoice -> !invoice.getPaidDate().isBefore(monthStart) && !invoice.getPaidDate().isAfter(monthEnd))
                    .map(invoice -> invoice.getTotalAmount())
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
        var recentInvoices = invoiceRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(invoiceService::toResponse)
                .toList();

        return new DashboardStatsResponse(
                revenueThisMonth,
                bookingRepository.countByEventDateBetween(start, end),
                outstandingPayments,
                occupancyRate(start, end),
                revenue,
                bookingCounts,
                upcoming,
                recentInvoices
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
        return invoiceRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(invoiceService::toResponse)
                .toList();
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
