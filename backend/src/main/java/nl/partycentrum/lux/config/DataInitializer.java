package nl.partycentrum.lux.config;

import nl.partycentrum.lux.domain.Role;
import nl.partycentrum.lux.domain.User;
import nl.partycentrum.lux.repository.UserRepository;
import nl.partycentrum.lux.service.CompanySettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanySettingsService companySettingsService;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CompanySettingsService companySettingsService
    ) {
        this.userRepository = userRepository;
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
