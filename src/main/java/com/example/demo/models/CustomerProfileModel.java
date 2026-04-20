package com.example.demo.models;

import com.example.demo.models.enums.CustomerStatus;

public class CustomerProfileModel {

    private int id;
    private int userId;
    private String bsn;
    private String phoneNumber;
    private CustomerStatus status;

    public CustomerProfileModel(int id, int userId, String bsn, String phoneNumber, CustomerStatus status) {
        this.id = id;
        this.userId = userId;
        this.bsn = bsn;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBsn() { return bsn; }
    public void setBsn(String bsn) { this.bsn = bsn; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}