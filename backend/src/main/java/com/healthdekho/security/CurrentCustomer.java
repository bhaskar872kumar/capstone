package com.healthdekho.security;

import com.healthdekho.common.error.UnauthorizedException;
import com.healthdekho.customer.Customer;
import com.healthdekho.customer.CustomerRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The logged-in customer, from the Spring Security session.
 * Services always use this id and never one sent by the client (FR-1 A: data restricted to the authenticated customer).
 */
@Component
public class CurrentCustomer {

    private final CustomerRepository customerRepository;

    public CurrentCustomer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Long id() {
        return find()
                .map(Customer::getId)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHENTICATED", "Please log in to continue."));
    }

    public Optional<Customer> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        // The username Spring Security stores is the customer's email (see SecurityConfig).
        return customerRepository.findByEmailIgnoreCase(authentication.getName());
    }
}
