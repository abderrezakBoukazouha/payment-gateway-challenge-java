package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ClientAuthorizationResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;

  private final CreditCardHiderService creditCardHiderService;

  private final AcquirerClient acquirerClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      CreditCardHiderService creditCardHiderService,
      AcquirerClient acquirerClient) {
    this.paymentsRepository = paymentsRepository;
    this.creditCardHiderService = creditCardHiderService;
    this.acquirerClient = acquirerClient;
  }

  public Optional<PostPaymentResponse> getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id);
  }

  public CompletableFuture<PostPaymentResponse> processPayment(PaymentRequest request,
      String idempotencyKey) {
    UUID idempotencyKeyUuid = UUID.fromString(idempotencyKey);

    // find the Payment in the local database, if not request a new payment to the bank
    return getPaymentById(idempotencyKeyUuid)
        .map(CompletableFuture::completedFuture)
        .orElseGet(
            () -> acquirerClient.authorizePayment(request, idempotencyKeyUuid)
                .thenApply(
                    clientAuthorizationResponse -> handleBankResponse(clientAuthorizationResponse,
                        request,
                        idempotencyKey)))
        // if the bank reject the payment, return REJECTED response
        .exceptionally(ex -> handleBankResponseError(request));
  }

  private PostPaymentResponse handleBankResponse(
      ClientAuthorizationResponse clientAuthorizationResponse,
      PaymentRequest paymentRequest, String idempotencyKey) {

    PaymentStatus paymentStatus =
        clientAuthorizationResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    // Save to database if it was a new successful process
    if (PaymentStatus.AUTHORIZED.equals(paymentStatus)) {
      LOG.info("Payment {} authorized by bank. Saving to registry.", idempotencyKey);

      paymentsRepository.add(buildPaymentStructure(paymentRequest, idempotencyKey, paymentStatus));
    }

    return buildPaymentStructure(paymentRequest, clientAuthorizationResponse.authorizationCode(),
        paymentStatus);
  }

  private PostPaymentResponse handleBankResponseError(PaymentRequest request) {
    return buildPaymentStructure(request, "", PaymentStatus.REJECTED);
  }

  private PostPaymentResponse buildPaymentStructure(PaymentRequest paymentRequest,
      String id, PaymentStatus paymentStatus) {
    UUID bankAuthorizationCodeResponse =
        id.isEmpty() ? null : UUID.fromString(id);

    return new PostPaymentResponse(bankAuthorizationCodeResponse, paymentStatus,
        creditCardHiderService.hide(paymentRequest.cardNumber()), paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(), paymentRequest.currency(), paymentRequest.amount());
  }
}
