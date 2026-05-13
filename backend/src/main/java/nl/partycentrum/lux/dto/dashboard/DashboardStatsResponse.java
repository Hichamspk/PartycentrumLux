package nl.partycentrum.lux.dto.dashboard;

import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
        BigDecimal revenueThisMonth,
        long bookingsThisMonth,
        BigDecimal outstandingPayments,
        double occupancyRate,
        List<ChartPoint> revenuePerMonth,
        List<ChartPoint> bookingsPerMonth,
        List<BookingResponse> upcomingBookings,
        List<InvoiceResponse> recentInvoices
) {
}
