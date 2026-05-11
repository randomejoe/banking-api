package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public int getUserId() {
        return user.getId();
    }
}
