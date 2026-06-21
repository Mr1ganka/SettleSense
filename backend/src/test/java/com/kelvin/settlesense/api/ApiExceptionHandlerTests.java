package com.kelvin.settlesense.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kelvin.settlesense.domain.model.dto.ErrorResponse;
import com.kelvin.settlesense.exceptions.BadCredentialsException;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void badCredentialsReturnsUnauthorized() {
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

        ErrorResponse response = handler.badCredentials(exception);

        assertThat(response.message()).isEqualTo("Invalid credentials");
    }

    @Test
    void illegalArgumentReturnsBadRequest() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

        ErrorResponse response = handler.illegalArgument(exception);

        assertThat(response.message()).isEqualTo("Invalid input");
    }
}
