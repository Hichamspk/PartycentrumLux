package nl.partycentrum.lux.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String naam;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone", nullable = false)
    private String telefoon;

    @Column(name = "address")
    private String adres;

    public Long getId() {
        return id;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getName() {
        return naam;
    }

    public void setName(String name) {
        this.naam = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefoon() {
        return telefoon;
    }

    public void setTelefoon(String telefoon) {
        this.telefoon = telefoon;
    }

    public String getPhone() {
        return telefoon;
    }

    public void setPhone(String phone) {
        this.telefoon = phone;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public String getAddress() {
        return adres;
    }

    public void setAddress(String address) {
        this.adres = address;
    }
}
