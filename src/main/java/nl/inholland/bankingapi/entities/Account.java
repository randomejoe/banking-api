package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account", indexes = {
        @Index(name = "idx_account_user_id", columnList = "user_id"),
        @Index(name = "idx_account_type",    columnList = "type"),
        @Index(name = "idx_account_status",  columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
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

    public int getUserId() {
        return user.getId();
    }
}
