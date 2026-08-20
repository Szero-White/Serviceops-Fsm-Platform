package com.serviceops.identity.web;

import com.serviceops.identity.domain.UserRole;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPasswordValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void userAccountApiRejectsPasswordShorterThanEightCharacters() {
        var request = new UserManagementController.UserAccountRequest(
                "owner.two",
                "Owner Two",
                UserRole.OWNER,
                "1234567",
                true,
                null,
                null
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }
}
