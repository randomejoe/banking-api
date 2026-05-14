package nl.inholland.bankingapi.entities.enums;

import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {
    CUSTOMER,
    EMPLOYEE;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
