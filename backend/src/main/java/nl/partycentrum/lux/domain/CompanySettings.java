package nl.partycentrum.lux.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_settings")
public class CompanySettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "logo_base64", columnDefinition = "TEXT")
    private String logoBase64;

    @Column(name = "brand_color", nullable = false)
    private String brandColor;

    @Column(nullable = false)
    private String address;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String kvk;

    @Column(name = "vat_number", nullable = false)
    private String vatNumber;

    @Column(nullable = false)
    private String iban;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String website;

    @Column(name = "mail_from", nullable = false)
    private String mailFrom;

    @Column(name = "docuseal_api_key")
    private String docusealApiKey;

    @Column(name = "docuseal_base_url", nullable = false)
    private String docusealBaseUrl;

    @Column(name = "docuseal_contract_template_id")
    private String docusealContractTemplateId;

    @Column(name = "general_terms", nullable = false, columnDefinition = "TEXT")
    private String generalTerms;

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public void setBrandColor(String brandColor) {
        this.brandColor = brandColor;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getKvk() {
        return kvk;
    }

    public void setKvk(String kvk) {
        this.kvk = kvk;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getDocusealApiKey() {
        return docusealApiKey;
    }

    public void setDocusealApiKey(String docusealApiKey) {
        this.docusealApiKey = docusealApiKey;
    }

    public String getDocusealBaseUrl() {
        return docusealBaseUrl;
    }

    public void setDocusealBaseUrl(String docusealBaseUrl) {
        this.docusealBaseUrl = docusealBaseUrl;
    }

    public String getDocusealContractTemplateId() {
        return docusealContractTemplateId;
    }

    public void setDocusealContractTemplateId(String docusealContractTemplateId) {
        this.docusealContractTemplateId = docusealContractTemplateId;
    }

    public String getGeneralTerms() {
        return generalTerms;
    }

    public void setGeneralTerms(String generalTerms) {
        this.generalTerms = generalTerms;
    }
}
