package nl.partycentrum.lux.controller;

import jakarta.validation.Valid;
import nl.partycentrum.lux.domain.BezichtigingStatus;
import nl.partycentrum.lux.dto.bezichtiging.BezichtigingRequest;
import nl.partycentrum.lux.dto.bezichtiging.BezichtigingResponse;
import nl.partycentrum.lux.dto.bezichtiging.BezichtigingStatusRequest;
import nl.partycentrum.lux.service.BezichtigingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/bezichtigingen")
public class BezichtigingController {

    private final BezichtigingService bezichtigingService;

    public BezichtigingController(BezichtigingService bezichtigingService) {
        this.bezichtigingService = bezichtigingService;
    }

    @GetMapping
    public List<BezichtigingResponse> findAll(@RequestParam(required = false) BezichtigingStatus status) {
        return status == null ? bezichtigingService.findAll() : bezichtigingService.findByStatus(status);
    }

    @GetMapping("/upcoming")
    public List<BezichtigingResponse> upcoming() {
        return bezichtigingService.findUpcoming();
    }

    @GetMapping("/calendar")
    public List<BezichtigingResponse> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return bezichtigingService.findCalendar(start, end);
    }

    @GetMapping("/{id}")
    public BezichtigingResponse findById(@PathVariable Long id) {
        return bezichtigingService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BezichtigingResponse> create(@Valid @RequestBody BezichtigingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bezichtigingService.create(request));
    }

    @PutMapping("/{id}")
    public BezichtigingResponse update(@PathVariable Long id, @Valid @RequestBody BezichtigingRequest request) {
        return bezichtigingService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public BezichtigingResponse updateStatus(@PathVariable Long id, @Valid @RequestBody BezichtigingStatusRequest request) {
        return bezichtigingService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/link-boeking/{boekingId}")
    public BezichtigingResponse linkToBoeking(@PathVariable Long id, @PathVariable Long boekingId) {
        return bezichtigingService.linkToBoeking(id, boekingId);
    }

    @GetMapping("/{id}/ics")
    public ResponseEntity<byte[]> ics(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bezichtiging-" + id + ".ics").build().toString())
                .body(bezichtigingService.generateIcsFile(id));
    }
}
