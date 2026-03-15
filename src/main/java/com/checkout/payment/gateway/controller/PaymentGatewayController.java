package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@RequestMapping("v1")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    PostPaymentResponse payment = paymentGatewayService.getPaymentById(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
    return new ResponseEntity<>(payment, HttpStatus.OK);
  }

  @PostMapping("/payment")
  public CompletableFuture<ResponseEntity<PostPaymentResponse>> processPaymentWithId(
      @RequestHeader(value = "X-Idempotency-Key") String idempotencyKey,
      @RequestBody @Valid PaymentRequest paymentRequest) {

    return paymentGatewayService.processPayment(paymentRequest, idempotencyKey)
        .thenApply(ResponseEntity::ok);
  }
}
