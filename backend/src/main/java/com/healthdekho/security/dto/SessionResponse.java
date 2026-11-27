package com.healthdekho.security.dto;

import com.healthdekho.customer.Customer;

/** Whether the browser has a logged-in session, and who it belongs to. */
public record SessionResponse(boolean authenticated, CustomerSummary customer) {

    public static SessionResponse loggedIn(Customer customer) {
        return new SessionResponse(true,
                new CustomerSummary(customer.getId(), customer.getFullName(), customer.getEmail()));
    }

    public static SessionResponse loggedOut() {
        return new SessionResponse(false, null);
    }

    public record CustomerSummary(Long id, String fullName, String email) {
    }
}
