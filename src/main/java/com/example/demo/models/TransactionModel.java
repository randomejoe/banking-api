package com.example.demo.models;

import com.example.demo.models.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionModel {

    private int id;
    private String fromIban;
    private String toIban;
    private int initiatedByUserId;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime timestamp;

    public TransactionModel(int id, String fromIban, String toIban, int initiatedByUserId, BigDecimal amount, TransactionType type, String description, LocalDateTime timestamp) {
        this.id = id;
        this.fromIban = fromIban;
        this.toIban = toIban;
        this.initiatedByUserId = initiatedByUserId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFromIban() { return fromIban; }
    public void setFromIban(String fromIban) { this.fromIban = fromIban; }

    public String getToIban() { return toIban; }
    public void setToIban(String toIban) { this.toIban = toIban; }

    public int getInitiatedByUserId() { return initiatedByUserId; }
    public void setInitiatedByUserId(int initiatedByUserId) { this.initiatedByUserId = initiatedByUserId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}