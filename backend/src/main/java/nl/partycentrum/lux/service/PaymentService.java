package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.domain.InvoiceType;
import nl.partycentrum.lux.domain.Payment;
import nl.partycentrum.lux.domain.PaymentPart;
import nl.partycentrum.lux.domain.PaymentState;
import nl.partycentrum.lux.dto.payment.MarkPaymentRequest;
import nl.partycentrum.lux.dto.payment.PaymentRequest;
import nl.partycentrum.lux.dto.payment.PaymentResponse;
import nl.partycentrum.lux.dto.payment.PaymentScheduleResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.InvoiceRepository;
import nl.partycentrum.lux.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final MailService mailService;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            BookingRepository bookingRepository,
            MailService mailService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> findPaymentSchedules(PaymentState status, String search) {
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CONCEPT)
                .filter(booking -> booking.getStatus() != BookingStatus.OFFERTE_VERZONDEN)
                .filter(booking -> booking.getStatus() != BookingStatus.GEANNULEERD)
                .filter(booking -> search == null || search.isBlank()
                        || booking.getCustomer().getNaam().toLowerCase().contains(search.toLowerCase()))
                .flatMap(booking -> List.of(
                        toSchedule(booking, PaymentPart.AANBETALING),
                        toSchedule(booking, PaymentPart.RESTANT)
                ).stream())
                .filter(payment -> status == null || payment.status() == status)
                .sorted(Comparator.comparing(PaymentScheduleResponse::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> findPaymentSchedulesByBooking(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
        return List.of(
                toSchedule(booking, PaymentPart.AANBETALING),
                toSchedule(booking, PaymentPart.RESTANT)
        );
    }

    @Transactional
    public PaymentScheduleResponse markBookingPaymentPaid(Long bookingId, MarkPaymentRequest request) {
        var type = request.type() == null ? PaymentPart.AANBETALING : request.type();
        return markBookingPaymentPaid(bookingId, type, request.betaaldDatum());
    }

    @Transactional
    public PaymentScheduleResponse markBookingPaymentPaid(Long bookingId, PaymentPart type, LocalDate paidDate) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
        if (booking.getStatus() == BookingStatus.GEANNULEERD) {
            throw new ApiException(HttpStatus.CONFLICT, "Een geannuleerde boeking kan niet betaald worden.");
        }
        var date = paidDate == null ? LocalDate.now() : paidDate;
        if (type == PaymentPart.RESTANT && !booking.isAanbetalingBetaald()) {
            throw new ApiException(HttpStatus.CONFLICT, "Restantbetaling is beschikbaar na ontvangst van de aanbetaling.");
        }

        if (type == PaymentPart.AANBETALING) {
            booking.setAanbetalingBetaald(true);
            booking.setAanbetalingBetaaldDatum(date);
            if (!booking.isRestantBetaald()) {
                booking.setStatus(BookingStatus.AANBETALING_BETAALD);
            }
        } else {
            booking.setRestantBetaald(true);
            booking.setRestantBetaaldDatum(date);
            mailService.sendReviewRequest(booking);
        }

        if (booking.isAanbetalingBetaald() && booking.isRestantBetaald()) {
            booking.setStatus(BookingStatus.VOLLEDIG_BETAALD);
        }
        mailService.sendPaymentConfirmation(booking, type);
        return toSchedule(booking, type);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        return toResponse(getPayment(id));
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        var invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Factuur niet gevonden."));

        var payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNotes(request.notes());
        var saved = paymentRepository.save(payment);

        var totalPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.BETAALD);
            invoice.setPaidDate(LocalDate.now());
            mailService.sendPaymentConfirmation(invoice);
        }
        reconcileBooking(invoice.getBooking().getId());

        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        var payment = getPayment(id);
        var invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Factuur niet gevonden."));
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNotes(request.notes());
        reconcileInvoice(invoice.getId());
        reconcileBooking(invoice.getBooking().getId());
        return toResponse(payment);
    }

    @Transactional
    public void delete(Long id) {
        var payment = getPayment(id);
        var invoiceId = payment.getInvoice().getId();
        var bookingId = payment.getInvoice().getBooking().getId();
        paymentRepository.delete(payment);
        reconcileInvoice(invoiceId);
        reconcileBooking(bookingId);
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Betaling niet gevonden."));
    }

    public PaymentResponse toResponse(Payment payment) {
        var invoice = payment.getInvoice();
        var booking = invoice.getBooking();
        return new PaymentResponse(
                payment.getId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                booking.getId(),
                booking.getCustomer().getName(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private void reconcileInvoice(Long invoiceId) {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Factuur niet gevonden."));
        var totalPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.BETAALD);
            invoice.setPaidDate(LocalDate.now());
        } else if (invoice.getDueDate().isBefore(LocalDate.now())) {
            invoice.setStatus(InvoiceStatus.VERLOPEN);
            invoice.setPaidDate(null);
        } else {
            invoice.setStatus(InvoiceStatus.ONBETAALD);
            invoice.setPaidDate(null);
        }
    }

    private void reconcileBooking(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
        if (booking.getStatus() == BookingStatus.GEANNULEERD) {
            return;
        }
        var invoices = invoiceRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        if (invoices.isEmpty()) {
            return;
        }
        if (invoices.stream().allMatch(invoice -> invoice.getStatus() == InvoiceStatus.BETAALD)) {
            booking.setStatus(BookingStatus.VOLLEDIG_BETAALD);
            mailService.sendBookingFullyPaid(booking);
            return;
        }
        if (invoices.stream().anyMatch(invoice -> invoice.getInvoiceType() == InvoiceType.AANBETALING
                && invoice.getStatus() == InvoiceStatus.BETAALD)) {
            booking.setStatus(BookingStatus.AANBETALING_BETAALD);
        }
    }

    private PaymentScheduleResponse toSchedule(Booking booking, PaymentPart type) {
        var paid = type == PaymentPart.AANBETALING ? booking.isAanbetalingBetaald() : booking.isRestantBetaald();
        var paidDate = type == PaymentPart.AANBETALING ? booking.getAanbetalingBetaaldDatum() : booking.getRestantBetaaldDatum();
        var deadline = type == PaymentPart.AANBETALING ? booking.getAanbetalingDeadline() : booking.getRestantDeadline();
        var amount = type == PaymentPart.AANBETALING ? booking.getAanbetalingBedrag() : booking.getRestantBedrag();
        var locked = type == PaymentPart.RESTANT && !booking.isAanbetalingBetaald();
        var state = paymentState(paid, deadline);

        return new PaymentScheduleResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getCustomer().getNaam(),
                booking.getCustomer().getNaam(),
                booking.getCustomer().getEmail(),
                booking.getEvenementDatum(),
                booking.getEvenementType(),
                booking.getStatus(),
                type,
                amount,
                deadline,
                state,
                paidDate,
                locked
        );
    }

    private PaymentState paymentState(boolean paid, LocalDate deadline) {
        if (paid) {
            return PaymentState.BETAALD;
        }
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            return PaymentState.VERLOPEN;
        }
        return PaymentState.OPENSTAAND;
    }
}
