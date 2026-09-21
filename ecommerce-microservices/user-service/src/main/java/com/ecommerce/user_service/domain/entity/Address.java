package com.ecommerce.user_service.domain.entity;

import jakarta.persistence.*;

// Address is the "many" side — many addresses belong to one user
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String country;

    // LAZY — don't load User when we only need the Address
    // @ManyToOne is EAGER by default — we override to LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // FK column in addresses table
    private User user;

    protected Address() {}

    public Address(String street, String city, String state, String zipCode, String country, User user) {
        this.street  = street;
        this.city    = city;
        this.state   = state;
        this.zipCode = zipCode;
        this.country = country;
        this.user    = user;
    }

    public Long getId()       { return id; }
    public String getStreet() { return street; }
    public String getCity()   { return city; }
    public String getState()  { return state; }
    public String getZipCode(){ return zipCode; }
    public String getCountry(){ return country; }
    public User getUser()     { return user; }

    public void setStreet(String street)   { this.street = street; }
    public void setCity(String city)       { this.city = city; }
    public void setState(String state)     { this.state = state; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public void setCountry(String country) { this.country = country; }
}
