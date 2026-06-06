package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIban(String iban);

    boolean existsByIban(String iban);

    List<Account> findByUser_Id(int userId);

    Page<Account> findByUser_Id(int userId, Pageable pageable);

    @Query("""
            SELECT a FROM Account a
            JOIN a.user u
            WHERE a.user.id <> :currentUserId
              AND a.type = nl.inholland.bankingapi.entities.enums.AccountType.CHECKING
              AND a.status = nl.inholland.bankingapi.entities.enums.AccountStatus.ACTIVE
              AND (
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
              )
            """)
    Page<Account> findTransferTargetsByCustomerName(@Param("currentUserId") int currentUserId,
                                                    @Param("name") String name,
                                                    Pageable pageable);

    default Page<Account> findAllFiltered(AccountQuery query, Pageable pageable) {
        return findAll(AccountSpecification.fromQuery(query), pageable);
    }
}
