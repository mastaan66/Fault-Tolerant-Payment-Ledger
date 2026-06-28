package io.github.mastaan66.ledger;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "One or more request fields are invalid");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
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
