package com.healthdekho.customer;

import com.healthdekho.customer.dto.CustomerProfileResponse;
import com.healthdekho.security.CurrentCustomer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentCustomer currentCustomer;

    public CustomerController(CustomerService customerService, CurrentCustomer currentCustomer) {
        this.customerService = customerService;
        this.currentCustomer = currentCustomer;
    }

    /** Logged-in customer's details; FR-3 uses them to pre-populate the registration form. */
    @GetMapping("/me")
    public CustomerProfileResponse me() {
        return customerService.profile(currentCustomer.id());
    }
}
