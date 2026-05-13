package nl.partycentrum.lux.repository;

import nl.partycentrum.lux.domain.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
}
