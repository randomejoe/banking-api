package com.example.demo.models;

import com.example.demo.models.enums.AccountStatus;
import com.example.demo.models.enums.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountModel {

    private int id;
    private int userId;
    private String iban;
    private AccountType type;
    private BigDecimal balance;
    private BigDecimal absoluteTransferLimit;
    private BigDecimal dailyTransferLimit;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public AccountModel(int id, int userId, String iban, AccountType type, BigDecimal balance, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit, AccountStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.iban = iban;
        this.type = type;
        this.balance = balance;
        this.absoluteTransferLimit = absoluteTransferLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getAbsoluteTransferLimit() { return absoluteTransferLimit; }
    public void setAbsoluteTransferLimit(BigDecimal absoluteTransferLimit) { this.absoluteTransferLimit = absoluteTransferLimit; }

    public BigDecimal getDailyTransferLimit() { return dailyTransferLimit; }
    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) { this.dailyTransferLimit = dailyTransferLimit; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}