package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.ContractStatus;
import nl.partycentrum.lux.domain.Invoice;
import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.domain.InvoiceType;
import nl.partycentrum.lux.domain.Payment;
import nl.partycentrum.lux.domain.PaymentMethod;
import nl.partycentrum.lux.dto.invoice.InvoiceRequest;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.InvoiceRepository;
import nl.partycentrum.lux.repository.PaymentRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.21");
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PdfInvoiceService pdfInvoiceService;
    private final MailService mailService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PdfInvoiceService pdfInvoiceService,
            MailService mailService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.pdfInvoiceService = pdfInvoiceService;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(InvoiceStatus status) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> status == null || invoice.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findByBooking(Long bookingId) {
        return invoiceRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return toResponse(getInvoice(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        var booking = getBooking(request.bookingId());
        var invoice = buildInvoice(
                booking,
                request.invoiceType() == null ? InvoiceType.VOLLEDIG : request.invoiceType(),
                money(request.amount()),
                request.dueDate(),
                InvoiceStatus.CONCEPT
        );
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public Invoice createForSignedContract(Booking booking) {
        var existing = invoiceRepository.findFirstByBookingIdOrderByCreatedAtAsc(booking.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        return invoiceRepository.save(buildInvoice(
                booking,
                InvoiceType.VOLLEDIG,
                money(booking.getPrice()),
                LocalDate.now().plusDays(30),
                InvoiceStatus.CONCEPT
        ));
    }

    @Transactional
    public List<InvoiceResponse> createOptionA(Long bookingId) {
        var booking = requireSignedBooking(bookingId);
        replaceUnpaidInvoices(booking);
        var invoice = invoiceRepository.save(buildInvoice(
                booking,
                InvoiceType.VOLLEDIG,
                money(booking.getPrice()),
                LocalDate.now().plusDays(30),
                InvoiceStatus.CONCEPT
        ));
        generatePdfIfNeeded(invoice);
        return List.of(toResponse(invoice));
    }

    @Transactional
    public List<InvoiceResponse> createOptionB(Long bookingId) {
        var booking = requireSignedBooking(bookingId);
        replaceUnpaidInvoices(booking);
        var subtotal = money(booking.getPrice());
        var deposit = money(subtotal.multiply(DEPOSIT_RATE));
        var remainder = money(subtotal.subtract(deposit));
        var depositInvoice = invoiceRepository.save(buildInvoice(
                booking,
                InvoiceType.AANBETALING,
                deposit,
                LocalDate.now().plusDays(7),
                InvoiceStatus.CONCEPT
        ));
        var remainderInvoice = invoiceRepository.save(buildInvoice(
                booking,
                InvoiceType.RESTANT,
                remainder,
                restDueDate(booking),
                InvoiceStatus.CONCEPT
        ));
        generatePdfIfNeeded(depositInvoice);
        generatePdfIfNeeded(remainderInvoice);
        return List.of(toResponse(depositInvoice), toResponse(remainderInvoice));
    }

    @Transactional
    public List<InvoiceResponse> sendForBooking(Long bookingId) {
        var booking = requireSignedBooking(bookingId);
        var invoices = invoiceRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        if (invoices.isEmpty()) {
            invoices = createOptionA(bookingId).stream().map(response -> getInvoice(response.id())).toList();
        }
        invoices.forEach(invoice -> {
            generatePdfIfNeeded(invoice);
            if (invoice.getStatus() == InvoiceStatus.CONCEPT) {
                invoice.setStatus(InvoiceStatus.ONBETAALD);
            }
            mailService.sendInvoice(invoice);
        });
        booking.setStatus(BookingStatus.FACTUUR_VERZONDEN);
        return invoices.stream().map(this::toResponse).toList();
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        var invoice = getInvoice(id);
        var booking = getBooking(request.bookingId());
        invoice.setBooking(booking);
        invoice.setInvoiceType(request.invoiceType() == null ? invoice.getInvoiceType() : request.invoiceType());
        applyAmounts(invoice, money(request.amount()));
        invoice.setDueDate(request.dueDate());
        invoice.setPdfPath(null);
        return toResponse(invoice);
    }

    @Transactional
    public void delete(Long id) {
        invoiceRepository.delete(getInvoice(id));
    }

    @Transactional
    public InvoiceResponse generatePdf(Long id) {
        var invoice = getInvoice(id);
        generatePdf(invoice);
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Resource downloadPdf(Long id) {
        var invoice = getInvoice(id);
        if (invoice.getPdfPath() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Voor deze factuur is nog geen PDF gegenereerd.");
        }

        try {
            var path = Path.of(invoice.getPdfPath()).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "PDF bestand niet gevonden op de server.");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF download link is ongeldig.");
        }
    }

    @Transactional
    public InvoiceResponse markPaid(Long id) {
        var invoice = getInvoice(id);
        var totalPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        var remaining = invoice.getTotalAmount().subtract(totalPaid);
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            var payment = new Payment();
            payment.setInvoice(invoice);
            payment.setAmount(remaining);
            payment.setPaymentDate(LocalDate.now());
            payment.setPaymentMethod(PaymentMethod.BANK);
            payment.setNotes("Automatisch geregistreerd bij markeren als betaald.");
            paymentRepository.save(payment);
        }
        invoice.setStatus(InvoiceStatus.BETAALD);
        invoice.setPaidDate(LocalDate.now());
        mailService.sendPaymentConfirmation(invoice);
        reconcileBookingPaymentStatus(invoice.getBooking());
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse sendReminder(Long id) {
        var invoice = getInvoice(id);
        mailService.sendPaymentReminder(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public void markOverdueInvoices() {
        invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.ONBETAALD, LocalDate.now())
                .forEach(invoice -> invoice.setStatus(InvoiceStatus.VERLOPEN));
    }

    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Factuur niet gevonden."));
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getBooking().getId(),
                invoice.getBooking().getCustomer().getName(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getInvoiceDate(),
                invoice.getAmount(),
                invoice.getVatAmount(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getDueDate(),
                invoice.getPaidDate(),
                invoice.getPdfPath(),
                invoice.getPdfPath() == null ? null : "/api/invoices/" + invoice.getId() + "/download",
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    private Booking requireSignedBooking(Long bookingId) {
        var booking = getBooking(bookingId);
        if (booking.getContractStatus() != ContractStatus.ONDERTEKEND
                && booking.getStatus() != BookingStatus.CONTRACT_ONDERTEKEND
                && booking.getStatus() != BookingStatus.FACTUUR_VERZONDEN
                && booking.getStatus() != BookingStatus.AANBETALING_BETAALD
                && booking.getStatus() != BookingStatus.VOLLEDIG_BETAALD
                && booking.getStatus() != BookingStatus.AFGEROND) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract moet ondertekend zijn voordat een factuur verstuurd kan worden.");
        }
        return booking;
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
    }

    private Invoice buildInvoice(Booking booking, InvoiceType type, BigDecimal amount, LocalDate dueDate, InvoiceStatus status) {
        var invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(status);
        invoice.setDueDate(dueDate);
        applyAmounts(invoice, amount);
        return invoice;
    }

    private void applyAmounts(Invoice invoice, BigDecimal amount) {
        var subtotal = money(amount);
        var vat = money(subtotal.multiply(VAT_RATE));
        invoice.setAmount(subtotal);
        invoice.setVatAmount(vat);
        invoice.setTotalAmount(money(subtotal.add(vat)));
    }

    private void replaceUnpaidInvoices(Booking booking) {
        var invoices = invoiceRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        var hasPaid = invoices.stream().anyMatch(invoice -> invoice.getStatus() == InvoiceStatus.BETAALD);
        if (hasPaid) {
            throw new ApiException(HttpStatus.CONFLICT, "Factuuroptie kan niet worden gewijzigd nadat er al betaald is.");
        }
        invoiceRepository.deleteAll(invoices);
    }

    private LocalDate restDueDate(Booking booking) {
        var fourteenDaysBefore = booking.getEventDate().minusDays(14);
        return fourteenDaysBefore.isAfter(LocalDate.now()) ? fourteenDaysBefore : LocalDate.now().plusDays(7);
    }

    private void generatePdfIfNeeded(Invoice invoice) {
        if (invoice.getPdfPath() == null) {
            generatePdf(invoice);
        }
    }

    private void generatePdf(Invoice invoice) {
        var pdfBytes = pdfInvoiceService.generate(invoice);
        var path = pdfInvoiceService.save(invoice, pdfBytes);
        invoice.setPdfPath(path.toString());
    }

    private void reconcileBookingPaymentStatus(Booking booking) {
        var invoices = invoiceRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        if (invoices.isEmpty() || booking.getStatus() == BookingStatus.GEANNULEERD) {
            return;
        }
        var allPaid = invoices.stream().allMatch(invoice -> invoice.getStatus() == InvoiceStatus.BETAALD);
        if (allPaid) {
            booking.setStatus(BookingStatus.VOLLEDIG_BETAALD);
            mailService.sendBookingFullyPaid(booking);
            return;
        }
        var depositPaid = invoices.stream()
                .anyMatch(invoice -> invoice.getInvoiceType() == InvoiceType.AANBETALING && invoice.getStatus() == InvoiceStatus.BETAALD);
        if (depositPaid) {
            booking.setStatus(BookingStatus.AANBETALING_BETAALD);
        }
    }

    private String nextInvoiceNumber() {
        var year = LocalDate.now().getYear();
        var prefix = "LUX-" + year + "-";
        var next = invoiceRepository.findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(prefix)
                .map(invoice -> invoice.getInvoiceNumber().substring(prefix.length()))
                .map(Integer::parseInt)
                .map(number -> number + 1)
                .orElse(1);
        return prefix + String.format("%03d", next);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
