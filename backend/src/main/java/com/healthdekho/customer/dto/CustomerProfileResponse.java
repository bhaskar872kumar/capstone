package com.healthdekho.customer.dto;

import com.healthdekho.customer.Customer;

import java.time.LocalDate;

public record CustomerProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth) {

    public static CustomerProfileResponse from(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getDateOfBirth());
    }
}
