package nl.partycentrum.lux.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import nl.partycentrum.lux.config.JwtProperties;
import nl.partycentrum.lux.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return generateToken(user, JwtTokenType.ACCESS, Duration.ofMinutes(properties.accessTokenExpirationMinutes()));
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, JwtTokenType.REFRESH, Duration.ofDays(properties.refreshTokenExpirationDays()));
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public JwtTokenType extractType(String token) {
        return JwtTokenType.valueOf(claims(token).get("type", String.class));
    }

    public boolean isValid(String token, UserDetails userDetails, JwtTokenType expectedType) {
        var claims = claims(token);
        return userDetails.getUsername().equals(claims.getSubject())
                && JwtTokenType.valueOf(claims.get("type", String.class)) == expectedType
                && claims.getExpiration().after(new Date());
    }

    private String generateToken(User user, JwtTokenType type, Duration duration) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .claim("type", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration)))
                .signWith(signingKey)
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
