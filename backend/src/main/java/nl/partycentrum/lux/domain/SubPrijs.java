package nl.partycentrum.lux.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "sub_prijzen")
public class SubPrijs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private String naam;

    @Column(nullable = false)
    private BigDecimal bedrag = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal prijs = BigDecimal.ZERO;

    @Column(nullable = false)
    private int position;

    @PrePersist
    @PreUpdate
    public void syncLegacyPrijs() {
        if (bedrag == null) {
            bedrag = BigDecimal.ZERO;
        }
        bedrag = bedrag.setScale(2, RoundingMode.HALF_UP);
        prijs = bedrag;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public BigDecimal getBedrag() {
        return bedrag == null ? BigDecimal.ZERO : bedrag;
    }

    public void setBedrag(BigDecimal bedrag) {
        this.bedrag = money(bedrag);
        this.prijs = this.bedrag;
    }

    public BigDecimal getPrijs() {
        return getBedrag();
    }

    public void setPrijs(BigDecimal prijs) {
        setBedrag(prijs);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }
}
