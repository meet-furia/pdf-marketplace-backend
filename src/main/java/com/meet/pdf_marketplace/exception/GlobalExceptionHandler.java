package com.meet.pdf_marketplace.exception;

import com.meet.pdf_marketplace.dto.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ResourceNotFoundException.class
    })
    public ResponseEntity<ApiResponseDTO<?>> handleNotFoundException(
            RuntimeException exception
    ) {

        return new ResponseEntity<>(
                ApiResponseDTO.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponseDTO<?>> handleBadRequestException(
            RuntimeException exception
    ) {

        return new ResponseEntity<>(
                ApiResponseDTO.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler({
            AuthenticationCredentialsNotFoundException.class
    })
    public ResponseEntity<ApiResponseDTO<?>> handleUnauthorizedException(
            RuntimeException exception
    ) {

        return new ResponseEntity<>(
                ApiResponseDTO.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return new ResponseEntity<>(
                ApiResponseDTO.builder()
                        .success(false)
                        .message(message)
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<?>> handleGenericException(
            Exception exception
    ) {

        return new ResponseEntity<>(
                ApiResponseDTO.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}