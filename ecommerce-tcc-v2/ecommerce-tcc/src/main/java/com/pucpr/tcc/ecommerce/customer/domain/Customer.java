package com.pucpr.tcc.ecommerce.customer.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean active;

    protected Customer() {}

    public Customer(String name, String email) {
        validateEmail(email);
        this.name = name;
        this.email = email;
        this.active = true;
    }

    // ---- Business Rules (PITest targets) ----

    public void validateIsActive() {
        if (!Boolean.TRUE.equals(this.active)) {
            throw new InactiveCustomerException(this.id);
        }
    }

    public void updateEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
    }

    public void deactivate() { this.active = false; }

    public void activate() { this.active = true; }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new InvalidEmailException(email);
        }
        int atIndex = email.indexOf('@');
        if (atIndex == 0) throw new InvalidEmailException(email);
        String domain = email.substring(atIndex + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            throw new InvalidEmailException(email);
        }
    }

    public boolean isActive() { return Boolean.TRUE.equals(active); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Boolean getActive() { return active; }
    public void setName(String name) { this.name = name; }
}
