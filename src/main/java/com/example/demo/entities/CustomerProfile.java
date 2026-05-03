package com.example.demo.entities;

import com.example.demo.entities.enums.CustomerStatus;
import jakarta.persistence.*;

@Entity
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String bsn;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    public CustomerProfile() {}

    public CustomerProfile(int id, User user, String bsn, String phoneNumber, CustomerStatus status) {
        this.id = id;
        this.user = user;
        this.bsn = bsn;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getUserId() { return user.getId(); }

    public String getBsn() { return bsn; }
    public void setBsn(String bsn) { this.bsn = bsn; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}
