package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    Account findByIban(String iban);

    List<Account> findByUser_Id(int userId);

    List<Account> findByUser_IdIn(List<Integer> userIds);

    @Query("SELECT a FROM Account a WHERE " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:status IS NULL OR a.status = :status)")
    List<Account> findByFilters(@Param("userId") Integer userId,
                                @Param("type") AccountType type,
                                @Param("status") AccountStatus status);
}
