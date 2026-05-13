package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.dashboard.DashboardStatsResponse;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;
import nl.partycentrum.lux.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('OWNER')")
    public DashboardStatsResponse stats() {
        return dashboardService.stats();
    }

    @GetMapping("/upcoming-bookings")
    public List<BookingResponse> upcomingBookings() {
        return dashboardService.upcomingBookings();
    }

    @GetMapping("/recent-invoices")
    @PreAuthorize("hasRole('OWNER')")
    public List<InvoiceResponse> recentInvoices() {
        return dashboardService.recentInvoices();
    }
}
