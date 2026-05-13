package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.ContractStatus;
import nl.partycentrum.lux.dto.contract.ContractResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final CompanySettingsService companySettingsService;
    private final ContractTemplateService contractTemplateService;
    private final DocusealService docusealService;
    private final InvoiceService invoiceService;
    private final MailService mailService;
    private final PdfInvoiceService pdfInvoiceService;

    public ContractService(
            BookingRepository bookingRepository,
            BookingService bookingService,
            CompanySettingsService companySettingsService,
            ContractTemplateService contractTemplateService,
            DocusealService docusealService,
            InvoiceService invoiceService,
            MailService mailService,
            PdfInvoiceService pdfInvoiceService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.companySettingsService = companySettingsService;
        this.contractTemplateService = contractTemplateService;
        this.docusealService = docusealService;
        this.invoiceService = invoiceService;
        this.mailService = mailService;
        this.pdfInvoiceService = pdfInvoiceService;
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long bookingId) {
        return toResponse(bookingService.getBooking(bookingId));
    }

    @Transactional
    public ContractResponse generate(Long bookingId) {
        var booking = bookingService.getBooking(bookingId);
        var html = contractTemplateService.render(booking, companySettingsService.resolve());
        booking.setContractHtml(html);
        booking.setContractStatus(ContractStatus.CONCEPT);
        return toResponse(booking);
    }

    @Transactional
    public ContractResponse saveConcept(Long bookingId, String html) {
        var booking = bookingService.getBooking(bookingId);
        booking.setContractHtml(resolveHtml(booking, html));
        booking.setContractStatus(ContractStatus.CONCEPT);
        return toResponse(booking);
    }

    @Transactional
    public ContractResponse send(Long bookingId, String html) {
        var booking = bookingService.getBooking(bookingId);
        var renderedHtml = resolveHtml(booking, html);
        var submission = docusealService.sendContract(booking, renderedHtml);
        booking.setContractHtml(renderedHtml);
        booking.setDocusealSubmissionId(submission.submissionId());
        booking.setContractStatus(ContractStatus.VERZONDEN);
        booking.setStatus(BookingStatus.OFFERTE_VERZONDEN);
        mailService.sendContractSigningRequest(booking, submission.signingUrl());
        return toResponse(booking);
    }

    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        var submissionId = extractSubmissionId(payload);
        if (submissionId == null || submissionId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DocuSeal webhook bevat geen submission id.");
        }
        var booking = bookingRepository.findByDocusealSubmissionId(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Geen boeking gevonden voor DocuSeal submission."));
        markSigned(booking);
    }

    @Transactional
    public ContractResponse markSignedForTest(Long bookingId) {
        var booking = bookingService.getBooking(bookingId);
        markSigned(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public byte[] signedContractPdf(Long bookingId) {
        var booking = bookingService.getBooking(bookingId);
        if (booking.getContractStatus() != ContractStatus.ONDERTEKEND) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract is nog niet ondertekend.");
        }
        return pdfInvoiceService.renderHtml(resolveHtml(booking, booking.getContractHtml()));
    }

    private void markSigned(Booking booking) {
        booking.setContractStatus(ContractStatus.ONDERTEKEND);
        booking.setContractSignedDate(LocalDate.now());
        booking.setStatus(BookingStatus.BEVESTIGD);
        invoiceService.createForSignedContract(booking);
        mailService.sendContractSignedConfirmation(booking);
    }

    private ContractResponse toResponse(Booking booking) {
        return new ContractResponse(
                booking.getId(),
                booking.getContractStatus(),
                booking.getContractHtml(),
                booking.getDocusealSubmissionId(),
                booking.getContractSignedDate()
        );
    }

    private String resolveHtml(Booking booking, String html) {
        if (html != null && !html.isBlank()) {
            return html;
        }
        if (booking.getContractHtml() != null && !booking.getContractHtml().isBlank()) {
            return booking.getContractHtml();
        }
        return contractTemplateService.render(booking, companySettingsService.resolve());
    }

    private String extractSubmissionId(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (var key : List.of("submission_id", "submissionId", "submissionId".toLowerCase(), "id")) {
                var direct = map.get(key);
                if (direct != null) {
                    return String.valueOf(direct);
                }
            }
            for (var nested : map.values()) {
                var found = extractSubmissionId(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (var item : list) {
                var found = extractSubmissionId(item);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
