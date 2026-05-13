package nl.partycentrum.lux.controller;

import jakarta.validation.Valid;
import nl.partycentrum.lux.domain.PaymentState;
import nl.partycentrum.lux.dto.payment.MarkPaymentRequest;
import nl.partycentrum.lux.dto.payment.PaymentScheduleResponse;
import nl.partycentrum.lux.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentScheduleResponse> findAll(
            @RequestParam(required = false) PaymentState status,
            @RequestParam(required = false) String search
    ) {
        return paymentService.findPaymentSchedules(status, search);
    }

    @GetMapping("/bookings/{bookingId}")
    public List<PaymentScheduleResponse> findByBooking(@PathVariable Long bookingId) {
        return paymentService.findPaymentSchedulesByBooking(bookingId);
    }

    @PostMapping("/bookings/{bookingId}/mark-paid")
    @PreAuthorize("hasRole('OWNER')")
    public PaymentScheduleResponse markPaid(
            @PathVariable Long bookingId,
            @Valid @RequestBody MarkPaymentRequest request
    ) {
        return paymentService.markBookingPaymentPaid(bookingId, request);
    }
}
