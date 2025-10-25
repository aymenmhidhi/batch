package com.example.batch.io;

import java.time.LocalDate;

public class CustomerRecord {

    private final String externalId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final LocalDate registrationDate;

    public CustomerRecord(String externalId, String firstName, String lastName, String email, LocalDate registrationDate) {
        this.externalId = externalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.registrationDate = registrationDate;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }
}
