package nl.partycentrum.lux.controller;

import jakarta.validation.Valid;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.dto.booking.BookingCancelRequest;
import nl.partycentrum.lux.dto.booking.BookingRequest;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.booking.BookingStatusRequest;
import nl.partycentrum.lux.dto.subprijs.SubPrijsRequest;
import nl.partycentrum.lux.dto.subprijs.SubPrijsResponse;
import nl.partycentrum.lux.service.AnnuleringService;
import nl.partycentrum.lux.service.BookingService;
import nl.partycentrum.lux.service.SubPrijsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final SubPrijsService subPrijsService;
    private final AnnuleringService annuleringService;

    public BookingController(
            BookingService bookingService,
            SubPrijsService subPrijsService,
            AnnuleringService annuleringService
    ) {
        this.bookingService = bookingService;
        this.subPrijsService = subPrijsService;
        this.annuleringService = annuleringService;
    }

    @GetMapping
    public List<BookingResponse> findAll(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String customer
    ) {
        return bookingService.findAll(status, eventType, startDate, endDate, customer);
    }

    @GetMapping("/calendar")
    public List<BookingResponse> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return bookingService.findCalendar(start, end);
    }

    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable Long id, @Valid @RequestBody BookingRequest request) {
        return bookingService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(@PathVariable Long id, @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status());
    }

    @GetMapping("/{id}/subprijzen")
    public List<SubPrijsResponse> subPrijzen(@PathVariable Long id) {
        return subPrijsService.findByBooking(id);
    }

    @PostMapping("/{id}/subprijzen")
    public ResponseEntity<SubPrijsResponse> createSubPrijs(
            @PathVariable Long id,
            @Valid @RequestBody SubPrijsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subPrijsService.create(id, request));
    }

    @PutMapping("/{bookingId}/subprijzen/{subPrijsId}")
    public SubPrijsResponse updateSubPrijs(
            @PathVariable Long bookingId,
            @PathVariable Long subPrijsId,
            @Valid @RequestBody SubPrijsRequest request
    ) {
        return subPrijsService.update(bookingId, subPrijsId, request);
    }

    @DeleteMapping("/{bookingId}/subprijzen/{subPrijsId}")
    public ResponseEntity<Void> deleteSubPrijs(@PathVariable Long bookingId, @PathVariable Long subPrijsId) {
        subPrijsService.delete(bookingId, subPrijsId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('OWNER')")
    public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody BookingCancelRequest request) {
        return annuleringService.cancel(id, request.resolvedReason());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
