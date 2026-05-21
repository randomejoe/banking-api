package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.TransactionType;

import java.math.BigDecimal;

public class TransactionFilterParams {

    private Integer customerId;
    private String iban;
    private TransactionType type;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
}
