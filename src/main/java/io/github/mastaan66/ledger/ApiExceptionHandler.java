package io.github.mastaan66.ledger;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, TransferNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict", exception.getMessage());
    }

    @ExceptionHandler({InsufficientFundsException.class, InvalidTransferException.class})
    ProblemDetail handleUnprocessable(RuntimeException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Transfer cannot be processed",
                exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return validationProblem(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.putIfAbsent(field, violation.getMessage());
        });
        return validationProblem(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(exception.getName(), "has an unsupported value");
        return validationProblem(errors);
    }

    private ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "One or more request fields are invalid");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(IncompleteIdempotencyRecordException.class)
    ProblemDetail handleIncompleteRecord(IncompleteIdempotencyRecordException exception) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Transfer result is temporarily unavailable",
                exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setType(URI.create("https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger"
                + "/blob/main/docs/API.md#errors"));
        return detail;
    }
}
