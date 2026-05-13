package nl.partycentrum.lux.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "price", nullable = false)
    private BigDecimal priceSnapshot = BigDecimal.ZERO;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<SubPrijs> subPrijzen = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal korting = BigDecimal.ZERO;

    @Column(name = "aanbetaling_percentage", nullable = false)
    private int aanbetalingPercentage = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONCEPT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ElementCollection
    @CollectionTable(name = "booking_properties", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "property", nullable = false)
    private List<String> properties = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", nullable = false)
    private ContractStatus contractStatus = ContractStatus.GEEN;

    @Column(name = "docuseal_submission_id")
    private String docusealSubmissionId;

    @Column(name = "contract_html", columnDefinition = "TEXT")
    private String contractHtml;

    @Column(name = "contract_signed_date")
    private LocalDate contractSignedDate;

    @Column(name = "offerte_datum")
    private LocalDate offerteDatum;

    @Column(name = "offerte_sent_date")
    private LocalDate offerteSentDate;

    @Column(name = "ondertekening_datum")
    private LocalDate ondertekeningDatum;

    @Column(name = "aanbetaling_betaald", nullable = false)
    private boolean aanbetalingBetaald;

    @Column(name = "aanbetaling_betaald_datum")
    private LocalDate aanbetalingBetaaldDatum;

    @Column(name = "restant_betaald", nullable = false)
    private boolean restantBetaald;

    @Column(name = "restant_betaald_datum")
    private LocalDate restantBetaaldDatum;

    @Column(name = "offerte_pdf_path")
    private String offertePdfPath;

    @Column(name = "annulerings_reden", columnDefinition = "TEXT")
    private String annuleringsReden;

    @PrePersist
    @PreUpdate
    public void syncCalculatedColumns() {
        priceSnapshot = getTotaal();
        if (offerteDatum == null && status == BookingStatus.OFFERTE_VERZONDEN) {
            offerteDatum = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getEvenementDatum() {
        return eventDate;
    }

    public void setEvenementDatum(LocalDate evenementDatum) {
        this.eventDate = evenementDatum;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getDate() {
        return eventDate;
    }

    public void setDate(LocalDate date) {
        this.eventDate = date;
    }

    public LocalDate getEndDate() {
        return eventDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.eventDate = endDate;
    }

    public LocalTime getStartTijd() {
        return startTime;
    }

    public void setStartTijd(LocalTime startTijd) {
        this.startTime = startTijd;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEindTijd() {
        return endTime;
    }

    public void setEindTijd(LocalTime eindTijd) {
        this.endTime = eindTijd;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public EventType getEvenementType() {
        return eventType;
    }

    public void setEvenementType(EventType evenementType) {
        this.eventType = evenementType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public int getAantalGasten() {
        return guestCount;
    }

    public void setAantalGasten(int aantalGasten) {
        this.guestCount = aantalGasten;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public BigDecimal getPrice() {
        return getTotaal();
    }

    public void setPrice(BigDecimal price) {
        this.priceSnapshot = money(price);
    }

    public List<SubPrijs> getSubPrijzen() {
        return subPrijzen;
    }

    public void setSubPrijzen(List<SubPrijs> subPrijzen) {
        this.subPrijzen.clear();
        if (subPrijzen != null) {
            subPrijzen.forEach(this::addSubPrijs);
        }
    }

    public void addSubPrijs(SubPrijs subPrijs) {
        subPrijs.setBooking(this);
        subPrijzen.add(subPrijs);
    }

    public BigDecimal getSubtotaal() {
        if (subPrijzen.isEmpty()) {
            return money(priceSnapshot);
        }
        return subPrijzen.stream()
                .map(SubPrijs::getBedrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getKorting() {
        return money(korting);
    }

    public void setKorting(BigDecimal korting) {
        this.korting = money(korting);
    }

    public BigDecimal getTotaal() {
        var totaal = getSubtotaal().subtract(getKorting());
        if (totaal.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totaal.setScale(2, RoundingMode.HALF_UP);
    }

    public int getAanbetalingPercentage() {
        return aanbetalingPercentage;
    }

    public void setAanbetalingPercentage(int aanbetalingPercentage) {
        this.aanbetalingPercentage = Math.max(0, Math.min(100, aanbetalingPercentage));
    }

    public BigDecimal getAanbetalingBedrag() {
        return getTotaal()
                .multiply(BigDecimal.valueOf(aanbetalingPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRestantBedrag() {
        return getTotaal().subtract(getAanbetalingBedrag()).setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getAanbetalingDeadline() {
        return ondertekeningDatum == null ? null : ondertekeningDatum.plusDays(7);
    }

    public LocalDate getRestantDeadline() {
        return eventDate == null ? null : eventDate.minusDays(14);
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status == null ? BookingStatus.CONCEPT : status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties == null ? new ArrayList<>() : new ArrayList<>(properties);
    }

    public List<String> getEigenschappen() {
        return properties;
    }

    public void setEigenschappen(List<String> eigenschappen) {
        setProperties(eigenschappen);
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public ContractStatus getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(ContractStatus contractStatus) {
        this.contractStatus = contractStatus == null ? ContractStatus.GEEN : contractStatus;
    }

    public String getDocusealSubmissionId() {
        return docusealSubmissionId;
    }

    public void setDocusealSubmissionId(String docusealSubmissionId) {
        this.docusealSubmissionId = docusealSubmissionId;
    }

    public String getContractHtml() {
        return contractHtml;
    }

    public void setContractHtml(String contractHtml) {
        this.contractHtml = contractHtml;
    }

    public LocalDate getContractSignedDate() {
        return ondertekeningDatum != null ? ondertekeningDatum : contractSignedDate;
    }

    public void setContractSignedDate(LocalDate contractSignedDate) {
        this.contractSignedDate = contractSignedDate;
        this.ondertekeningDatum = contractSignedDate;
    }

    public LocalDate getOfferteDatum() {
        return offerteDatum;
    }

    public void setOfferteDatum(LocalDate offerteDatum) {
        this.offerteDatum = offerteDatum;
    }

    public LocalDate getOfferteSentDate() {
        return offerteSentDate;
    }

    public void setOfferteSentDate(LocalDate offerteSentDate) {
        this.offerteSentDate = offerteSentDate;
    }

    public LocalDate getOndertekeningDatum() {
        return ondertekeningDatum;
    }

    public void setOndertekeningDatum(LocalDate ondertekeningDatum) {
        this.ondertekeningDatum = ondertekeningDatum;
        this.contractSignedDate = ondertekeningDatum;
    }

    public boolean isAanbetalingBetaald() {
        return aanbetalingBetaald;
    }

    public void setAanbetalingBetaald(boolean aanbetalingBetaald) {
        this.aanbetalingBetaald = aanbetalingBetaald;
    }

    public LocalDate getAanbetalingBetaaldDatum() {
        return aanbetalingBetaaldDatum;
    }

    public void setAanbetalingBetaaldDatum(LocalDate aanbetalingBetaaldDatum) {
        this.aanbetalingBetaaldDatum = aanbetalingBetaaldDatum;
    }

    public boolean isRestantBetaald() {
        return restantBetaald;
    }

    public void setRestantBetaald(boolean restantBetaald) {
        this.restantBetaald = restantBetaald;
    }

    public LocalDate getRestantBetaaldDatum() {
        return restantBetaaldDatum;
    }

    public void setRestantBetaaldDatum(LocalDate restantBetaaldDatum) {
        this.restantBetaaldDatum = restantBetaaldDatum;
    }

    public String getOffertePdfPath() {
        return offertePdfPath;
    }

    public void setOffertePdfPath(String offertePdfPath) {
        this.offertePdfPath = offertePdfPath;
    }

    public String getAnnuleringsReden() {
        return annuleringsReden;
    }

    public void setAnnuleringsReden(String annuleringsReden) {
        this.annuleringsReden = annuleringsReden;
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }
}
