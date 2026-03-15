package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.CLientAuthorizationRequest;
import com.checkout.payment.gateway.model.ClientAuthorizationResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AcquirerClient {

  private static final Logger LOG = LoggerFactory.getLogger(AcquirerClient.class);

  private final RestTemplate restTemplate;
  private final Executor executor;

  private final String bankUrl;

  public AcquirerClient(RestTemplate restTemplate, Executor executor,
      @Value("${bank.api.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.executor = executor;
    this.bankUrl = bankUrl;
  }

  public CompletableFuture<ClientAuthorizationResponse> authorizePayment(PaymentRequest request,
      UUID idempotencyKey) {
    return CompletableFuture.supplyAsync(() -> executeBankCall(request, idempotencyKey), executor)
        .thenApply(response -> {
          LOG.info("Bank authorization successfully resolved for ID: {}", idempotencyKey);
          return response;
        })
        .exceptionally(ex -> {
          String errorMessage = "Bank authorization failed for ID %s: %s".formatted(idempotencyKey,
              ex.getMessage());
          LOG.error(errorMessage);
          throw new BankCommunicationException(errorMessage);
        });
  }

  private ClientAuthorizationResponse executeBankCall(PaymentRequest request, UUID idempotencyKey) {
    LOG.info("Attempting Bank authorization for key: {}", idempotencyKey);

    CLientAuthorizationRequest authorizationRequest = buildAuthorizationRequest(request);

    HttpEntity<CLientAuthorizationRequest> entity = new HttpEntity<>(authorizationRequest);

    ResponseEntity<ClientAuthorizationResponse> response = restTemplate.postForEntity(
        "%s/%s".formatted(bankUrl, "payments"), entity, ClientAuthorizationResponse.class);
    return response.getBody();
  }

  private CLientAuthorizationRequest buildAuthorizationRequest(PaymentRequest request) {
    return new CLientAuthorizationRequest(request.cardNumber(),
        "%s/%s".formatted(request.expiryMonth(), request.expiryYear()), request.currency(),
        request.amount(), request.cvv());
  }
}
