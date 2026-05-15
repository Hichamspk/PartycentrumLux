package nl.partycentrum.lux.dto.mail;

import nl.partycentrum.lux.domain.MailLogStatus;
import nl.partycentrum.lux.domain.MailLogType;

import java.time.LocalDateTime;

public record MailLogResponse(
        Long id,
        Long bookingId,
        Long bezichtigingId,
        MailLogType type,
        String klantNaam,
        String ontvangerEmail,
        String onderwerp,
        MailLogStatus status,
        String foutmelding,
        LocalDateTime verzondenOp
) {
}
