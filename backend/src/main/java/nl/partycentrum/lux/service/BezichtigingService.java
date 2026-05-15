package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Bezichtiging;
import nl.partycentrum.lux.domain.BezichtigingStatus;
import nl.partycentrum.lux.dto.bezichtiging.BezichtigingRequest;
import nl.partycentrum.lux.dto.bezichtiging.BezichtigingResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BezichtigingRepository;
import nl.partycentrum.lux.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BezichtigingService {

    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final BezichtigingRepository bezichtigingRepository;
    private final BookingRepository bookingRepository;
    private final MailService mailService;

    public BezichtigingService(
            BezichtigingRepository bezichtigingRepository,
            BookingRepository bookingRepository,
            MailService mailService
    ) {
        this.bezichtigingRepository = bezichtigingRepository;
        this.bookingRepository = bookingRepository;
        this.mailService = mailService;
    }

    @Transactional
    public BezichtigingResponse create(BezichtigingRequest request) {
        var bezichtiging = new Bezichtiging();
        apply(bezichtiging, request);
        var saved = bezichtigingRepository.save(bezichtiging);
        sendBevestigingsmail(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public BezichtigingResponse update(Long id, BezichtigingRequest request) {
        var bezichtiging = getBezichtiging(id);
        apply(bezichtiging, request);
        return toResponse(bezichtiging);
    }

    @Transactional
    public BezichtigingResponse updateStatus(Long id, BezichtigingStatus status) {
        var bezichtiging = getBezichtiging(id);
        bezichtiging.setStatus(status);
        return toResponse(bezichtiging);
    }

    @Transactional(readOnly = true)
    public List<BezichtigingResponse> findAll() {
        return bezichtigingRepository.findAllByOrderByDatumAscStartTijdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BezichtigingResponse findById(Long id) {
        return toResponse(getBezichtiging(id));
    }

    @Transactional(readOnly = true)
    public List<BezichtigingResponse> findByStatus(BezichtigingStatus status) {
        return bezichtigingRepository.findByStatusOrderByDatumAscStartTijdAsc(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BezichtigingResponse> findUpcoming() {
        return bezichtigingRepository.findByDatumGreaterThanEqualOrderByDatumAscStartTijdAsc(LocalDate.now()).stream()
                .filter(bezichtiging -> bezichtiging.getStatus() == BezichtigingStatus.GEPLAND)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BezichtigingResponse> findCalendar(LocalDate start, LocalDate end) {
        return bezichtigingRepository.findByDatumBetweenOrderByDatumAscStartTijdAsc(start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BezichtigingResponse linkToBoeking(Long bezichtigingId, Long boekingId) {
        var bezichtiging = getBezichtiging(bezichtigingId);
        var booking = bookingRepository.findById(boekingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
        bezichtiging.setBoeking(booking);
        return toResponse(bezichtiging);
    }

    @Transactional
    public void sendBevestigingsmail(Long bezichtigingId) {
        var bezichtiging = getBezichtiging(bezichtigingId);
        mailService.sendBezichtigingConfirmation(
                bezichtiging.getId(),
                bezichtiging.getKlantNaam(),
                bezichtiging.getKlantEmail(),
                bezichtiging.getDatum(),
                bezichtiging.getStartTijd(),
                bezichtiging.getEindTijd(),
                generateIcsFile(bezichtiging)
        );
    }

    @Transactional
    public void sendHerinnering(Long bezichtigingId) {
        var bezichtiging = getBezichtiging(bezichtigingId);
        mailService.sendBezichtigingReminder(
                bezichtiging.getId(),
                bezichtiging.getKlantNaam(),
                bezichtiging.getKlantEmail(),
                bezichtiging.getDatum(),
                bezichtiging.getStartTijd(),
                bezichtiging.getEindTijd()
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateIcsFile(Long bezichtigingId) {
        return generateIcsFile(getBezichtiging(bezichtigingId));
    }

    @Transactional
    public void sendHerinneringenVoorDatum(LocalDate datum) {
        bezichtigingRepository.findByDatumAndStatusOrderByStartTijdAsc(datum, BezichtigingStatus.GEPLAND)
                .forEach(bezichtiging -> sendHerinnering(bezichtiging.getId()));
    }

    public Bezichtiging getBezichtiging(Long id) {
        return bezichtigingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bezichtiging niet gevonden."));
    }

    public BezichtigingResponse toResponse(Bezichtiging bezichtiging) {
        var booking = bezichtiging.getBoeking();
        return new BezichtigingResponse(
                bezichtiging.getId(),
                bezichtiging.getKlantNaam(),
                bezichtiging.getKlantEmail(),
                bezichtiging.getKlantTelefoon(),
                bezichtiging.getDatum(),
                bezichtiging.getStartTijd(),
                bezichtiging.getEindTijd(),
                bezichtiging.getStatus(),
                bezichtiging.getNotities(),
                booking == null ? null : booking.getId(),
                booking == null ? null : booking.getCustomer().getNaam(),
                bezichtiging.getCreatedAt(),
                bezichtiging.getUpdatedAt()
        );
    }

    private void apply(Bezichtiging bezichtiging, BezichtigingRequest request) {
        if (!request.eindTijd().isAfter(request.startTijd())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Eindtijd moet na starttijd liggen.");
        }
        bezichtiging.setKlantNaam(request.klantNaam().trim());
        bezichtiging.setKlantEmail(request.klantEmail().trim());
        bezichtiging.setKlantTelefoon(request.klantTelefoon().trim());
        bezichtiging.setDatum(request.datum());
        bezichtiging.setStartTijd(request.startTijd());
        bezichtiging.setEindTijd(request.eindTijd());
        bezichtiging.setStatus(request.status() == null ? bezichtiging.getStatus() : request.status());
        bezichtiging.setNotities(blankToNull(request.notities()));
        if (request.bookingId() != null) {
            var booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
            bezichtiging.setBoeking(booking);
        }
    }

    private byte[] generateIcsFile(Bezichtiging bezichtiging) {
        var start = formatIcsDateTime(bezichtiging.getDatum(), bezichtiging.getStartTijd());
        var end = formatIcsDateTime(bezichtiging.getDatum(), bezichtiging.getEindTijd());
        var ics = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Partycentrum Lux//NL
                BEGIN:VEVENT
                DTSTART:%s
                DTEND:%s
                SUMMARY:Bezichtiging Partycentrum Lux - %s
                DESCRIPTION:Bezichtiging voor %s
                LOCATION:Bennebroekerweg 530, 2132MD Hoofddorp
                END:VEVENT
                END:VCALENDAR
                """.formatted(start, end, escapeIcs(bezichtiging.getKlantNaam()), escapeIcs(bezichtiging.getKlantNaam()));
        return ics.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private String formatIcsDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time).format(ICS_DATE_TIME);
    }

    private String escapeIcs(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
