package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.CustomerProfile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerProfileRepository extends CrudRepository<CustomerProfile, Integer> {

    //SELECT * FROM customer_profile WHERE user_id = ?
    CustomerProfile findByUser_Id(int userId);
}
