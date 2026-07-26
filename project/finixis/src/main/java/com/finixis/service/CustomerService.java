package com.finixis.service;

import com.finixis.model.Customer;
import com.finixis.repository.CustomerRepository;
import com.finixis.repository.impl.JdbcCustomerRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService() { this.repo = new JdbcCustomerRepository(); }

    public List<Customer> getAll()           { return repo.findAll(); }
    public Optional<Customer> getById(int id) { return repo.findById(id); }
    public List<Customer> search(String q)   { return q == null || q.isBlank() ? getAll() : repo.search(q); }
    public double getBalance(int customerId) { return repo.getBalance(customerId); }

    public Customer addCustomer(String name, String phone, String email, String address) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        c.setCustomerSince(LocalDate.now());
        return repo.save(c);
    }

    public Customer updateCustomer(Customer c) { return repo.save(c); }

    public void deleteCustomer(int id) { repo.delete(id); }

    /** Refresh the balance field of a customer object from the DB. */
    public void refreshBalance(Customer c) {
        c.setBalance(repo.getBalance(c.getId()));
    }
}
