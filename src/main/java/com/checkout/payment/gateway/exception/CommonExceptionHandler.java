package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<Map<String, Object>> handleException(EventProcessingException ex) {
    LOG.error("Payment id Not found {}", ex.getMessage());
    return buildRejectedResponse("Payment id Not found, %s".formatted(ex.getMessage()),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
    LOG.warn("Missing required header: {}", ex.getHeaderName());
    return buildRejectedResponse("Required header %s is missing".formatted(ex.getHeaderName()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> "%s:%s".formatted(error.getField(), error.getDefaultMessage()))
        .toList();

    return buildRejectedResponse(String.join(", ", errors), HttpStatus.BAD_REQUEST);
  }

  private ResponseEntity<Map<String, Object>> buildRejectedResponse(String reason,
      HttpStatus status) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", PaymentStatus.REJECTED.getName());
    body.put("reason", reason);
    body.put("timestamp", LocalDateTime.now().toString());

    return new ResponseEntity<>(body, status);
  }

}
