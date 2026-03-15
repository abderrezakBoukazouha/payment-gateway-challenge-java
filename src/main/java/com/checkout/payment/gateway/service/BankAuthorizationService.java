package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankAuthorizationRequest;
import com.checkout.payment.gateway.model.BankAuthorizationResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BankAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(BankAuthorizationService.class);

  private final CreditCardHiderService creditCardHiderService;
  private final RestTemplate restTemplate;
  private final Executor executor;

  private final String bankUrl ;

  public BankAuthorizationService(CreditCardHiderService creditCardHiderService,
      RestTemplate restTemplate, Executor executor,
      @Value("${bank.api.url}") String bankUrl) {
    this.creditCardHiderService = creditCardHiderService;
    this.restTemplate = restTemplate;
    this.executor = executor;
    this.bankUrl = bankUrl;
  }

  public CompletableFuture<BankAuthorizationResponse> authorizePayment(PaymentRequest request,
      UUID idempotencyKey) {
    return CompletableFuture.supplyAsync(() -> executeBankCall(request, idempotencyKey), executor);
  }

  @Retryable(retryFor = {
      RestClientException.class}, backoff = @Backoff(delay = 2000, multiplier = 2))
  public BankAuthorizationResponse executeBankCall(PaymentRequest request, UUID idempotencyKey) {
    LOG.info("Attempting bank authorization for key: {}", idempotencyKey);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Idempotency-Key", idempotencyKey.toString());
    headers.setContentType(MediaType.APPLICATION_JSON);

    BankAuthorizationRequest authorizationRequest = getAuthorizationRequest(request);

    HttpEntity<BankAuthorizationRequest> entity = new HttpEntity<>(authorizationRequest, headers);

    ResponseEntity<BankAuthorizationResponse> response = restTemplate.postForEntity("%s/%s".formatted(bankUrl, "payments"), entity,
        BankAuthorizationResponse.class);
    return response.getBody();
  }

  private BankAuthorizationRequest getAuthorizationRequest(PaymentRequest request) {
    return new BankAuthorizationRequest(request.cardNumber(),
        "%s/%s".formatted(request.expiryMonth(), request.expiryYear()), request.currency(),
        request.amount(), request.cvv());
  }

  @Recover
  public PostPaymentResponse recover(RestClientException e, PaymentRequest request,
      UUID idempotencyKey) {
    LOG.error("Bank is down or unreachable after 3 attempts for key {}. Failing gracefully.",
        idempotencyKey);

    return new PostPaymentResponse(idempotencyKey, PaymentStatus.DECLINED,
        creditCardHiderService.hide(request.cardNumber()), request.expiryMonth(),
        request.expiryYear(), request.currency(), request.amount());
  }
}
