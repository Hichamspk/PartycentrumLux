package nl.partycentrum.lux.dto.booking;

public record BookingCancelRequest(String reason, String annuleringsReden) {
    public String resolvedReason() {
        return annuleringsReden != null && !annuleringsReden.isBlank() ? annuleringsReden : reason;
    }
}
