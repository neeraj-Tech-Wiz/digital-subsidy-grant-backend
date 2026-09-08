package com.infosys.subsidy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException exception) {

        Map<String, String> error = new HashMap<>();

        error.put("error", "Invalid input format");
        error.put("message",
                "Please check that each field contains the correct data type.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DuplicateAadhaarException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateAadhaar(
            DuplicateAadhaarException exception) {

        Map<String, String> error = new HashMap<>();

        error.put("error", "Duplicate Aadhaar");
        error.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(DuplicateMobileException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateMobile(
            DuplicateMobileException exception) {

        Map<String, String> error = new HashMap<>();

        error.put("error", "Duplicate Mobile Number");
        error.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(SchemeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSchemeNotFound(
            SchemeNotFoundException exception) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Scheme Not Found");
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(DuplicateSchemeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateScheme(
            DuplicateSchemeException exception) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Duplicate Scheme");
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(InvalidCriteriaException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCriteria(
            InvalidCriteriaException exception) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Invalid Criteria Configuration");
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCustomValidation(
            ValidationException exception) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Validation Error");
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal Server Error OR Validation Failure");
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}