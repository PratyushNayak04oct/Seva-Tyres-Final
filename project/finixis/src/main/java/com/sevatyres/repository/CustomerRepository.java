package com.sevatyres.repository;

import com.sevatyres.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    List<Customer>     findAll();
    Optional<Customer> findById(int id);
    List<Customer>     search(String query);
    Customer           save(Customer customer);
    void               delete(int id);
    /** Net balance: positive = customer owes us; negative = we owe customer. */
    double             getBalance(int customerId);
}
