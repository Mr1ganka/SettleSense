package com.kelvin.settlesense.api;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse illegalArgument(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse validation(MethodArgumentNotValidException exception) {
		var message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(this::format)
				.orElse("request validation failed");
		return new ErrorResponse(message);
	}

	private String format(FieldError error) {
		return error.getField() + " " + error.getDefaultMessage();
	}

	record ErrorResponse(String message) {
	}
}
