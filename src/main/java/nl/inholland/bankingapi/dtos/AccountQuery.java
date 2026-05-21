package nl.inholland.bankingapi.dtos;

import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;

public class AccountQuery {

    private Integer userId;
    private AccountType type;
    private AccountStatus status;
    private String iban;
    private String name;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
