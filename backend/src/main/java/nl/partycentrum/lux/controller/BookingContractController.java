package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.dto.contract.ContractResponse;
import nl.partycentrum.lux.dto.contract.ContractSaveRequest;
import nl.partycentrum.lux.service.ContractService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/{bookingId}/contract")
@PreAuthorize("hasRole('OWNER')")
public class BookingContractController {

    private final ContractService contractService;

    public BookingContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ContractResponse get(@PathVariable Long bookingId) {
        return contractService.get(bookingId);
    }

    @PostMapping("/generate")
    public ContractResponse generate(@PathVariable Long bookingId) {
        return contractService.generate(bookingId);
    }

    @PutMapping("/concept")
    public ContractResponse saveConcept(@PathVariable Long bookingId, @RequestBody ContractSaveRequest request) {
        return contractService.saveConcept(bookingId, request.html());
    }

    @PostMapping("/send")
    public ContractResponse send(@PathVariable Long bookingId, @RequestBody ContractSaveRequest request) {
        return contractService.send(bookingId, request.html());
    }

    @PostMapping("/mark-signed")
    public ContractResponse markSigned(@PathVariable Long bookingId) {
        return contractService.markSignedForTest(bookingId);
    }

    @GetMapping("/download-signed")
    public ResponseEntity<byte[]> downloadSigned(@PathVariable Long bookingId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("getekend-contract-" + bookingId + ".pdf").build().toString())
                .body(contractService.signedContractPdf(bookingId));
    }
}
