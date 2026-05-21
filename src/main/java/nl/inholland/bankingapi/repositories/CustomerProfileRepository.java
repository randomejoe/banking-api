package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.CustomerProfile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerProfileRepository extends CrudRepository<CustomerProfile, Integer> {

    // SELECT * FROM customer_profile WHERE user_id = ?
    CustomerProfile findByUser_Id(int userId);

    // Batch-fetch profiles for a page of users — avoids N+1 in getAll
    List<CustomerProfile> findByUser_IdIn(List<Integer> userIds);
}
