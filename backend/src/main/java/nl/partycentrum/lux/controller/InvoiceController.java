package nl.partycentrum.lux.controller;

import jakarta.validation.Valid;
import nl.partycentrum.lux.domain.InvoiceStatus;
import nl.partycentrum.lux.dto.invoice.InvoiceRequest;
import nl.partycentrum.lux.dto.invoice.InvoiceResponse;
import nl.partycentrum.lux.service.InvoiceService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceResponse> findAll(@RequestParam(required = false) InvoiceStatus status) {
        return invoiceService.findAll(status);
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable Long id) {
        return invoiceService.findById(id);
    }

    @GetMapping("/booking/{bookingId}")
    public List<InvoiceResponse> findByBooking(@PathVariable Long bookingId) {
        return invoiceService.findByBooking(bookingId);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.update(id, request);
    }

    @PostMapping("/booking/{bookingId}/option-a")
    @PreAuthorize("hasRole('OWNER')")
    public List<InvoiceResponse> createOptionA(@PathVariable Long bookingId) {
        return invoiceService.createOptionA(bookingId);
    }

    @PostMapping("/booking/{bookingId}/option-b")
    @PreAuthorize("hasRole('OWNER')")
    public List<InvoiceResponse> createOptionB(@PathVariable Long bookingId) {
        return invoiceService.createOptionB(bookingId);
    }

    @PostMapping("/booking/{bookingId}/send")
    @PreAuthorize("hasRole('OWNER')")
    public List<InvoiceResponse> sendForBooking(@PathVariable Long bookingId) {
        return invoiceService.sendForBooking(bookingId);
    }

    @PostMapping("/{id}/generate-pdf")
    @PreAuthorize("hasRole('OWNER')")
    public InvoiceResponse generatePdf(@PathVariable Long id) {
        return invoiceService.generatePdf(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var resource = invoiceService.downloadPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(resource.getFilename()).build().toString())
                .body(resource);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('OWNER')")
    public InvoiceResponse markPaid(@PathVariable Long id) {
        return invoiceService.markPaid(id);
    }

    @PostMapping("/{id}/send-reminder")
    @PreAuthorize("hasRole('OWNER')")
    public InvoiceResponse sendReminder(@PathVariable Long id) {
        return invoiceService.sendReminder(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
