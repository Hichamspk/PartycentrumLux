package nl.partycentrum.lux.service;

import nl.partycentrum.lux.dto.auth.AuthResponse;
import nl.partycentrum.lux.dto.auth.LoginRequest;
import nl.partycentrum.lux.dto.auth.RefreshTokenRequest;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.UserRepository;
import nl.partycentrum.lux.security.JwtService;
import nl.partycentrum.lux.security.JwtTokenType;
import nl.partycentrum.lux.security.LuxUserDetailsService;
import nl.partycentrum.lux.security.LuxUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LuxUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            LuxUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Ongeldige inloggegevens.");
        }
        var user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Ongeldige inloggegevens."));
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            var email = jwtService.extractUsername(request.refreshToken());
            var userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isValid(request.refreshToken(), userDetails, JwtTokenType.REFRESH)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is ongeldig of verlopen.");
            }

            var user = ((LuxUserPrincipal) userDetails).user();
            return new AuthResponse(
                    jwtService.generateAccessToken(user),
                    jwtService.generateRefreshToken(user),
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole()
            );
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is ongeldig of verlopen.");
        }
    }
}
