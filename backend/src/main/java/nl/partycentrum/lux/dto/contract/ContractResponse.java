package nl.partycentrum.lux.dto.contract;

import nl.partycentrum.lux.domain.ContractStatus;

import java.time.LocalDate;

public record ContractResponse(
        Long bookingId,
        ContractStatus status,
        String html,
        String docusealSubmissionId,
        LocalDate signedDate
) {
}
