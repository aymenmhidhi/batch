package com.example.batch.config;

import com.example.batch.domain.Customer;
import com.example.batch.io.CustomerRecord;
import com.example.batch.repository.CustomerRepository;
import org.springframework.batch.item.ItemProcessor;

public class CustomerItemProcessor implements ItemProcessor<CustomerRecord, Customer> {

    private final CustomerRepository repository;

    public CustomerItemProcessor(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer process(CustomerRecord item) {
        return repository.findByExternalId(item.getExternalId())
                .map(existing -> updateExisting(existing, item))
                .orElseGet(() -> new Customer(
                        item.getExternalId(),
                        item.getFirstName(),
                        item.getLastName(),
                        item.getEmail(),
                        item.getRegistrationDate()
                ));
    }

    private Customer updateExisting(Customer existing, CustomerRecord item) {
        existing.setFirstName(item.getFirstName());
        existing.setLastName(item.getLastName());
        existing.setEmail(item.getEmail());
        existing.setRegistrationDate(item.getRegistrationDate());
        return existing;
    }
}
