package nl.partycentrum.lux.dto.offerte;

import java.util.List;

public record OfferteDraftRequest(
        List<String> eigenschappen,
        String extraVoorwaarden,
        String klantNotities
) {
}
