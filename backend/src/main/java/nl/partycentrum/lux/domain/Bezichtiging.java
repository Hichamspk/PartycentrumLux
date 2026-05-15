package nl.partycentrum.lux.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bezichtigingen")
public class Bezichtiging extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "klant_naam", nullable = false)
    private String klantNaam;

    @Column(name = "klant_email", nullable = false)
    private String klantEmail;

    @Column(name = "klant_telefoon", nullable = false)
    private String klantTelefoon;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(name = "start_tijd", nullable = false)
    private LocalTime startTijd;

    @Column(name = "eind_tijd", nullable = false)
    private LocalTime eindTijd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BezichtigingStatus status = BezichtigingStatus.GEPLAND;

    @Column(columnDefinition = "TEXT")
    private String notities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking boeking;

    public Long getId() {
        return id;
    }

    public String getKlantNaam() {
        return klantNaam;
    }

    public void setKlantNaam(String klantNaam) {
        this.klantNaam = klantNaam;
    }

    public String getKlantEmail() {
        return klantEmail;
    }

    public void setKlantEmail(String klantEmail) {
        this.klantEmail = klantEmail;
    }

    public String getKlantTelefoon() {
        return klantTelefoon;
    }

    public void setKlantTelefoon(String klantTelefoon) {
        this.klantTelefoon = klantTelefoon;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public LocalTime getStartTijd() {
        return startTijd;
    }

    public void setStartTijd(LocalTime startTijd) {
        this.startTijd = startTijd;
    }

    public LocalTime getEindTijd() {
        return eindTijd;
    }

    public void setEindTijd(LocalTime eindTijd) {
        this.eindTijd = eindTijd;
    }

    public BezichtigingStatus getStatus() {
        return status;
    }

    public void setStatus(BezichtigingStatus status) {
        this.status = status == null ? BezichtigingStatus.GEPLAND : status;
    }

    public String getNotities() {
        return notities;
    }

    public void setNotities(String notities) {
        this.notities = notities;
    }

    public Booking getBoeking() {
        return boeking;
    }

    public void setBoeking(Booking boeking) {
        this.boeking = boeking;
    }
}
