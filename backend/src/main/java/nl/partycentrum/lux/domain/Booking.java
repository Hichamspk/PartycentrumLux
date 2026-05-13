package nl.partycentrum.lux.domain;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
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

    @Column(nullable = false)
    private BigDecimal price;

    @OneToMany(mappedBy = "booking", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<SubPrijs> subPrijzen = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

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

    @Column(name = "annulerings_reden", columnDefinition = "TEXT")
    private String annuleringsReden;

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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
        this.contractStatus = contractStatus;
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
        return contractSignedDate;
    }

    public void setContractSignedDate(LocalDate contractSignedDate) {
        this.contractSignedDate = contractSignedDate;
    }

    public String getAnnuleringsReden() {
        return annuleringsReden;
    }

    public void setAnnuleringsReden(String annuleringsReden) {
        this.annuleringsReden = annuleringsReden;
    }
}
