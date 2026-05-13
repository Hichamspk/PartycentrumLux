package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.ContractStatus;
import nl.partycentrum.lux.dto.booking.BookingResponse;
import nl.partycentrum.lux.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final BookingService bookingService;

    public ContractController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> findAll(
            @RequestParam(required = false) ContractStatus contractStatus,
            @RequestParam(required = false) String customer
    ) {
        return bookingService.findAll(null, null, null, null, customer).stream()
                .filter(booking -> contractStatus == null || booking.contractStatus() == contractStatus)
                .filter(booking -> booking.status() != BookingStatus.GEANNULEERD || contractStatus == ContractStatus.ONDERTEKEND)
                .toList();
    }
}
