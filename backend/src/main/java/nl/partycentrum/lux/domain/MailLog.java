package nl.partycentrum.lux.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_logs")
public class MailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "bezichtiging_id")
    private Long bezichtigingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailLogType type;

    @Column(name = "ontvanger_email", nullable = false)
    private String ontvangerEmail;

    @Column(nullable = false)
    private String onderwerp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailLogStatus status;

    @Column(columnDefinition = "TEXT")
    private String foutmelding;

    @Column(name = "verzonden_op", nullable = false)
    private LocalDateTime verzondenOp;

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getBezichtigingId() {
        return bezichtigingId;
    }

    public void setBezichtigingId(Long bezichtigingId) {
        this.bezichtigingId = bezichtigingId;
    }

    public MailLogType getType() {
        return type;
    }

    public void setType(MailLogType type) {
        this.type = type;
    }

    public String getOntvangerEmail() {
        return ontvangerEmail;
    }

    public void setOntvangerEmail(String ontvangerEmail) {
        this.ontvangerEmail = ontvangerEmail;
    }

    public String getOnderwerp() {
        return onderwerp;
    }

    public void setOnderwerp(String onderwerp) {
        this.onderwerp = onderwerp;
    }

    public MailLogStatus getStatus() {
        return status;
    }

    public void setStatus(MailLogStatus status) {
        this.status = status;
    }

    public String getFoutmelding() {
        return foutmelding;
    }

    public void setFoutmelding(String foutmelding) {
        this.foutmelding = foutmelding;
    }

    public LocalDateTime getVerzondenOp() {
        return verzondenOp;
    }

    public void setVerzondenOp(LocalDateTime verzondenOp) {
        this.verzondenOp = verzondenOp;
    }
}
