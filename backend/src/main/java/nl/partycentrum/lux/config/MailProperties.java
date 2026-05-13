package nl.partycentrum.lux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lux.mail")
public record MailProperties(
        boolean enabled,
        String from,
        String ownerEmail
) {
}
