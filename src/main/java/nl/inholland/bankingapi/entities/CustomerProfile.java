package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "user")
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

    // Read-only navigation: fetch accounts for this profile's user
    // insertable/updatable = false prevents Hibernate from managing this FK column
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
            insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private List<Account> accounts;

    public CustomerProfile(int id, User user, String bsn, String phoneNumber, CustomerStatus status) {
        this.id = id;
        this.user = user;
        this.bsn = bsn;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public int getUserId() {
        return user.getId();
    }
}
