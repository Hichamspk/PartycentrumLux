package nl.partycentrum.lux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lux.invoice")
public record InvoiceProperties(String storageDir) {
}
