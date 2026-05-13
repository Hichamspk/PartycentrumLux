package nl.partycentrum.lux.config;

import nl.partycentrum.lux.domain.Booking;
import nl.partycentrum.lux.domain.BookingStatus;
import nl.partycentrum.lux.domain.Customer;
import nl.partycentrum.lux.domain.EventType;
import nl.partycentrum.lux.domain.Role;
import nl.partycentrum.lux.domain.SubPrijs;
import nl.partycentrum.lux.domain.User;
import nl.partycentrum.lux.repository.BookingRepository;
import nl.partycentrum.lux.repository.CustomerRepository;
import nl.partycentrum.lux.repository.UserRepository;
import nl.partycentrum.lux.service.CompanySettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanySettingsService companySettingsService;

    public DataInitializer(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            CompanySettingsService companySettingsService
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.companySettingsService = companySettingsService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        companySettingsService.resolve();
        if (userRepository.count() == 0) {
            createUser("Eigenaar Lux", "owner@partycentrumlux.nl", "LuxAdmin123!", Role.OWNER);
            createUser("Medewerker Lux", "employee@partycentrumlux.nl", "LuxEmployee123!", Role.EMPLOYEE);
        }

        if (customerRepository.count() == 0) {
            var klant = new Customer();
            klant.setName("Familie De Vries");
            klant.setEmail("devries@example.com");
            klant.setPhone("+31 6 12345678");
            klant.setAddress("Kerkstraat 14, Amsterdam");
            customerRepository.save(klant);

            var booking = new Booking();
            booking.setCustomer(klant);
            booking.setEventDate(LocalDate.now().plusDays(14));
            booking.setStartTime(LocalTime.of(18, 0));
            booking.setEndTime(LocalTime.of(23, 30));
            booking.setEventType(EventType.BRUILOFT);
            booking.setGuestCount(120);
            booking.setPrice(new BigDecimal("4250.00"));
            booking.setStatus(BookingStatus.CONTRACT_ONDERTEKEND);
            booking.setNotes("Witte decoratie, halal buffet, DJ vanaf 20:00.");
            booking.setProperties(List.of("Catering inbegrepen", "Decoratie inbegrepen", "Parkeren gratis"));
            booking.setConditions("Geluid buiten na 23:00 beperken volgens gemeentelijke regels.");
            var zaalhuur = new SubPrijs();
            zaalhuur.setNaam("Huur evenementenlocatie");
            zaalhuur.setPrijs(new BigDecimal("3500.00"));
            zaalhuur.setPosition(0);
            booking.addSubPrijs(zaalhuur);
            var catering = new SubPrijs();
            catering.setNaam("Cateringpakket");
            catering.setPrijs(new BigDecimal("750.00"));
            catering.setPosition(1);
            booking.addSubPrijs(catering);
            bookingRepository.save(booking);
        }
    }

    private void createUser(String name, String email, String password, Role role) {
        var user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
    }
}
