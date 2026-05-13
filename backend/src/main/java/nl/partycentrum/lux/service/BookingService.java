package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.domain.SubPrijs;
import nl.partycentrum.lux.dto.booking.BookingRequest;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.dto.subprijs.SubPrijsRequest;
import nl.partycentrum.lux.dto.subprijs.SubPrijsResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;

    public BookingService(
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            CustomerService customerService
    ) {
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAll(
            BookingStatus status,
            EventType eventType,
            LocalDate startDate,
            LocalDate endDate,
            String customer
    ) {
        return bookingRepository.findAll().stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> eventType == null || booking.getEventType() == eventType)
                .filter(booking -> startDate == null || !booking.getEventDate().isBefore(startDate))
                .filter(booking -> endDate == null || !booking.getEventDate().isAfter(endDate))
                .filter(booking -> customer == null || customer.isBlank()
                        || booking.getCustomer().getName().toLowerCase().contains(customer.toLowerCase()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return toResponse(getBooking(id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findCalendar(LocalDate start, LocalDate end) {
        return bookingRepository.findByEventDateBetweenOrderByEventDateAsc(start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByEventDateDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        var booking = new Booking();
        apply(booking, request);
        ensureAvailable(booking, null);
        var saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse update(Long id, BookingRequest request) {
        var booking = getBooking(id);
        apply(booking, request);
        ensureAvailable(booking, id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status) {
        var booking = getBooking(id);
        booking.setStatus(status);
        return toResponse(booking);
    }

    @Transactional
    public void delete(Long id) {
        bookingRepository.delete(getBooking(id));
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boeking niet gevonden."));
    }

    public BookingResponse toResponse(Booking booking) {
        var invoiceId = invoiceRepository.findFirstByBookingIdOrderByCreatedAtAsc(booking.getId()).map(invoice -> invoice.getId()).orElse(null);
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getCustomer().getName(),
                booking.getCustomer().getEmail(),
                booking.getCustomer().getPhone(),
                booking.getCustomer().getAddress(),
                booking.getEventDate(),
                booking.getEventDate(),
                booking.getEventDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getEventType(),
                booking.getGuestCount(),
                booking.getPrice(),
                booking.getSubPrijzen().stream().map(this::toSubPrijsResponse).toList(),
                booking.getStatus(),
                booking.getNotes(),
                List.copyOf(booking.getProperties()),
                booking.getConditions(),
                booking.getContractStatus(),
                booking.getDocusealSubmissionId(),
                booking.getContractSignedDate(),
                booking.getAnnuleringsReden(),
                invoiceId,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private void apply(Booking booking, BookingRequest request) {
        var eventDate = request.eventDate() != null ? request.eventDate() : request.date();
        if (eventDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Evenementdatum is verplicht.");
        }
        var startTime = request.startTime() == null ? LocalTime.of(18, 0) : request.startTime();
        var endTime = request.endTime() == null ? LocalTime.of(23, 0) : request.endTime();
        if (!endTime.isAfter(startTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "De eindtijd moet na de begintijd liggen.");
        }
        booking.setCustomer(customerService.getCustomer(request.customerId()));
        booking.setEventDate(eventDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setEventType(request.eventType());
        booking.setGuestCount(request.guestCount());
        booking.setSubPrijzen(toSubPrijzen(request.subPrijzen(), request.price()));
        booking.setPrice(totalSubPrijzen(booking));
        booking.setStatus(request.status() == null
                ? (booking.getStatus() == null ? BookingStatus.CONCEPT : booking.getStatus())
                : request.status());
        booking.setNotes(request.notes());
        booking.setProperties(cleanProperties(request.properties()));
        booking.setConditions(request.conditions());
    }

    private void ensureAvailable(Booking booking, Long currentId) {
        if (booking.getStatus() == BookingStatus.GEANNULEERD) {
            return;
        }

        var overlapping = bookingRepository
                .findByStatusNotAndEventDate(
                        BookingStatus.GEANNULEERD,
                        booking.getEventDate()
                )
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .findFirst();

        if (overlapping.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "De zaal is op deze datum al bezet.");
        }
    }

    private List<String> cleanProperties(List<String> properties) {
        if (properties == null) {
            return List.of();
        }
        return properties.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(property -> !property.isBlank())
                .distinct()
                .toList();
    }

    private List<SubPrijs> toSubPrijzen(List<SubPrijsRequest> subPrijzen, BigDecimal fallbackPrice) {
        if (subPrijzen == null || subPrijzen.isEmpty()) {
            var subPrijs = new SubPrijs();
            subPrijs.setNaam("Huur evenementenlocatie");
            subPrijs.setPrijs(money(fallbackPrice == null ? BigDecimal.ZERO : fallbackPrice));
            subPrijs.setPosition(0);
            return List.of(subPrijs);
        }

        var result = new java.util.ArrayList<SubPrijs>();
        for (var i = 0; i < subPrijzen.size(); i++) {
            var request = subPrijzen.get(i);
            if (request == null) {
                continue;
            }
            var naam = request.naam() == null ? "" : request.naam().trim();
            if (naam.isBlank()) {
                continue;
            }
            var subPrijs = new SubPrijs();
            subPrijs.setNaam(naam);
            subPrijs.setPrijs(money(request.prijs() == null ? BigDecimal.ZERO : request.prijs()));
            subPrijs.setPosition(request.position() == null ? i : request.position());
            result.add(subPrijs);
        }
        if (result.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimaal een subprijs is verplicht.");
        }
        return result;
    }

    private BigDecimal totalSubPrijzen(Booking booking) {
        return booking.getSubPrijzen().stream()
                .map(SubPrijs::getPrijs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private SubPrijsResponse toSubPrijsResponse(SubPrijs subPrijs) {
        return new SubPrijsResponse(subPrijs.getId(), subPrijs.getNaam(), subPrijs.getPrijs(), subPrijs.getPosition());
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
