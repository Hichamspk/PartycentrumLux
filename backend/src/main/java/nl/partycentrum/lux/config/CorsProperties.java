package nl.partycentrum.lux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "lux.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
