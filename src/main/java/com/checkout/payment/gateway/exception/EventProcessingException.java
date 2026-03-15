package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;

public class EventProcessingException extends RuntimeException{
  public EventProcessingException(String message) {
    super(message);
  }
}
