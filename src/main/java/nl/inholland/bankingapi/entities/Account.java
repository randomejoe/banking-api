package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal absoluteTransferLimit;

    @Column(nullable = false)
    private BigDecimal dailyTransferLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Account() {}

    public Account(int id, User user, String iban, AccountType type, BigDecimal balance, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit, AccountStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getUserId() { return user.getId(); }

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
