package nl.partycentrum.lux.security;

import nl.partycentrum.lux.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LuxUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LuxUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .map(LuxUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Onbekende gebruiker."));
    }
}
