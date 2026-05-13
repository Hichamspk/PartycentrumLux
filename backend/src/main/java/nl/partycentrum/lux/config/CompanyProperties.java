package nl.partycentrum.lux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lux.company")
public record CompanyProperties(String logoPath) {
}
