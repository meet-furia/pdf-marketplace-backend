package com.meet.pdf_marketplace.exception;

import com.meet.pdf_marketplace.dto.common.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation failures from request DTO annotations.
     * Returns field-level messages without stack traces.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        List<ErrorResponseDTO.FieldValidationError> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Request contains invalid fields",
                validationErrors
        );
    }

    /**
     * Handles missing resources.
     * Returns a 404 response with a clean error body.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFoundException(
            ResourceNotFoundException exception
    ) {

        return buildResponse(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    /**
     * Handles forbidden business operations.
     * Returns a 403 response for denied access.
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbiddenException(
            ForbiddenOperationException exception
    ) {

        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage());
    }

    /**
     * Handles bad request exceptions from business validation.
     * Returns a 400 response with the provided message.
     */
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBadRequestException(
            RuntimeException exception
    ) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage());
    }

    /**
     * Handles missing or invalid authentication.
     * Returns a 401 response without exposing security internals.
     */
    @ExceptionHandler({
            AuthenticationCredentialsNotFoundException.class,
            JwtException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(
            RuntimeException exception
    ) {

        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage());
    }

    /**
     * Handles all unexpected backend errors.
     * Returns a generic response so stack traces are not exposed.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception exception
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Something went wrong"
        );
    }

    private ErrorResponseDTO.FieldValidationError toValidationError(FieldError fieldError) {

        return ErrorResponseDTO.FieldValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status,
            String error,
            String message
    ) {

        return buildResponse(status, error, message, null);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status,
            String error,
            String message,
            List<ErrorResponseDTO.FieldValidationError> validationErrors
    ) {

        return new ResponseEntity<>(
                ErrorResponseDTO.builder()
                        .error(error)
                        .message(message)
                        .status(status.value())
                        .validationErrors(validationErrors)
                        .build(),
                status
        );
    }
}

