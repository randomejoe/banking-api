package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_from_iban",    columnList = "from_iban"),
        @Index(name = "idx_transaction_to_iban",      columnList = "to_iban"),
        @Index(name = "idx_transaction_type",         columnList = "type"),
        @Index(name = "idx_transaction_timestamp",    columnList = "timestamp"),
        @Index(name = "idx_transaction_initiated_by", columnList = "initiated_by_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "initiatedBy")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "from_iban")
    private String fromIban;

    @Column(name = "to_iban")
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

    public int getInitiatedByUserId() {
        return initiatedBy.getId();
    }
}
