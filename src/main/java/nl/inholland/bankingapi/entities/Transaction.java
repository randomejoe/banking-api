package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "from_iban", nullable = true)
    private String fromIban;

    @Column(name = "to_iban", nullable = true)
    private String toIban;

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    private User initiatedBy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public Transaction() {}

    public Transaction(int id, String fromIban, String toIban, User initiatedBy, BigDecimal amount, TransactionType type, String description, LocalDateTime timestamp) {
        this.id = id;
        this.fromIban = fromIban;
        this.toIban = toIban;
        this.initiatedBy = initiatedBy;
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

    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User initiatedBy) { this.initiatedBy = initiatedBy; }

    public int getInitiatedByUserId() { return initiatedBy.getId(); }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
