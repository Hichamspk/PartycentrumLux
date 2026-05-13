package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.SubPrijs;
import nl.partycentrum.lux.dto.subprijs.SubPrijsRequest;
import nl.partycentrum.lux.dto.subprijs.SubPrijsResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.SubPrijsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SubPrijsService {

    private final SubPrijsRepository subPrijsRepository;
    private final BookingService bookingService;

    public SubPrijsService(SubPrijsRepository subPrijsRepository, BookingService bookingService) {
        this.subPrijsRepository = subPrijsRepository;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public List<SubPrijsResponse> findByBooking(Long bookingId) {
        bookingService.getBooking(bookingId);
        return subPrijsRepository.findByBookingIdOrderByPositionAscIdAsc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubPrijsResponse create(Long bookingId, SubPrijsRequest request) {
        var booking = bookingService.getBooking(bookingId);
        var subPrijs = new SubPrijs();
        subPrijs.setBooking(booking);
        apply(subPrijs, request);
        var saved = subPrijsRepository.save(subPrijs);
        booking.setPrice(total(booking.getId()));
        return toResponse(saved);
    }

    @Transactional
    public SubPrijsResponse update(Long bookingId, Long id, SubPrijsRequest request) {
        var subPrijs = getForBooking(bookingId, id);
        apply(subPrijs, request);
        subPrijs.getBooking().setPrice(total(bookingId));
        return toResponse(subPrijs);
    }

    @Transactional
    public void delete(Long bookingId, Long id) {
        var subPrijs = getForBooking(bookingId, id);
        subPrijsRepository.delete(subPrijs);
        subPrijs.getBooking().setPrice(total(bookingId).subtract(subPrijs.getPrijs()).max(BigDecimal.ZERO));
    }

    private SubPrijs getForBooking(Long bookingId, Long id) {
        var subPrijs = subPrijsRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subprijs niet gevonden."));
        if (!subPrijs.getBooking().getId().equals(bookingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Subprijs hoort niet bij deze boeking.");
        }
        return subPrijs;
    }

    private void apply(SubPrijs subPrijs, SubPrijsRequest request) {
        if (request.naam() == null || request.naam().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Naam van subprijs is verplicht.");
        }
        subPrijs.setNaam(request.naam().trim());
        subPrijs.setBedrag(money(request.resolvedBedrag() == null ? BigDecimal.ZERO : request.resolvedBedrag()));
        subPrijs.setPosition(request.position() == null ? subPrijs.getPosition() : request.position());
    }

    private BigDecimal total(Long bookingId) {
        return subPrijsRepository.findByBookingIdOrderByPositionAscIdAsc(bookingId).stream()
                .map(SubPrijs::getBedrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private SubPrijsResponse toResponse(SubPrijs subPrijs) {
        return new SubPrijsResponse(subPrijs.getId(), subPrijs.getNaam(), subPrijs.getBedrag(), subPrijs.getBedrag(), subPrijs.getPosition());
    }
}
