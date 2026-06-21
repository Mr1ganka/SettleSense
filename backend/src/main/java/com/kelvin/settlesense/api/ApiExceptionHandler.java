package com.kelvin.settlesense.api;

import com.kelvin.settlesense.exceptions.ExpenseUpdateException;
import com.kelvin.settlesense.exceptions.GroupUpdateException;
import com.kelvin.settlesense.exceptions.UserUpdateException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kelvin.settlesense.exceptions.BadCredentialsException;
import com.kelvin.settlesense.domain.model.dto.ErrorResponse;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse illegalArgument(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(ExpenseUpdateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse expenseUpdate(ExpenseUpdateException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(GroupUpdateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse expenseUpdate(GroupUpdateException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(UserUpdateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorResponse expenseUpdate(UserUpdateException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	ErrorResponse badCredentials(BadCredentialsException exception) {
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

}
