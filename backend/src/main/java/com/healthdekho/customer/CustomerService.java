package com.healthdekho.customer;

import com.healthdekho.common.error.NotFoundException;
import com.healthdekho.customer.dto.CustomerProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse profile(Long customerId) {
        return customerRepository.findById(customerId)
                .map(CustomerProfileResponse::from)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer account not found."));
    }
}
