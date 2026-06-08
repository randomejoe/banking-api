package nl.inholland.bankingapi.entities;

import nl.inholland.bankingapi.entities.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_role",  columnList = "role"),
        @Index(name = "idx_user_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "passwordHash")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private LocalDateTime createdAt;

    // CustomerProfile stores the FK, so this side is just for navigation
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, optional = true)
    @Setter(AccessLevel.NONE)
    private CustomerProfile customerProfile;

    // Accounts reference this user via user_id
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private List<Account> accounts;

    public User(int id, String email, String passwordHash, String firstName, String lastName,
                UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdAt = createdAt;
    }

    @Override
    public List<UserRole> getAuthorities() {
        return List.of(role);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }
}
