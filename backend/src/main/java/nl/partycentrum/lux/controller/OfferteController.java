package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.dto.offerte.OfferteResponse;
import nl.partycentrum.lux.service.BookingService;
import nl.partycentrum.lux.service.OfferteService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/{bookingId}/offerte")
public class OfferteController {

    private final OfferteService offerteService;
    private final BookingService bookingService;

    public OfferteController(OfferteService offerteService, BookingService bookingService) {
        this.offerteService = offerteService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public OfferteResponse get(@PathVariable Long bookingId) {
        return offerteService.toResponse(bookingService.getBooking(bookingId));
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@PathVariable Long bookingId) {
        return offerteService.renderHtml(bookingService.getBooking(bookingId));
    }

    @PostMapping("/generate")
    public OfferteResponse generate(@PathVariable Long bookingId) {
        return offerteService.generate(bookingId);
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('OWNER')")
    public OfferteResponse send(@PathVariable Long bookingId) {
        return offerteService.send(bookingId);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@PathVariable Long bookingId) {
        var resource = offerteService.download(bookingId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("offerte-" + bookingId + ".pdf").build().toString())
                .body(resource);
    }
}
