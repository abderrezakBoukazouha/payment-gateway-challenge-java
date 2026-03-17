package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
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
      CreditCardHiderService creditCardHiderService, AcquirerClient acquirerClient) {
    this.paymentsRepository = paymentsRepository;
    this.creditCardHiderService = creditCardHiderService;
    this.acquirerClient = acquirerClient;
  }

  public Optional<PostPaymentResponse> getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id);
  }

  private Optional<PostPaymentResponse> getByIdempotencyKey(UUID id) {
    LOG.debug("Looking up cache to find payment with ID {}", id);
    return paymentsRepository.getByIdempotencyKey(id);
  }


  public CompletableFuture<PostPaymentResponse> processPayment(PaymentRequest request,
      String idempotencyKey) {
    UUID idempotencyKeyUuid;
    try {
      idempotencyKeyUuid = UUID.fromString(idempotencyKey);
    } catch (IllegalArgumentException e) {
      LOG.error("Invalid idempotency key provided", e.getMessage());
      throw new EventProcessingException("Invalid idempotency key provided");
    }

    // find the Payment in the local cache, and return the payment
    // if not request a new payment to the bank
    return getByIdempotencyKey(idempotencyKeyUuid).map(CompletableFuture::completedFuture)
        .orElseGet(() -> acquirerClient.authorizePayment(request, idempotencyKeyUuid).thenApply(
            clientAuthorizationResponse -> handleBankResponse(clientAuthorizationResponse, request,
                idempotencyKeyUuid)))
        // if the bank reject the payment, return REJECTED response
        .exceptionally(ex -> handleBankResponseError(request, idempotencyKeyUuid));
  }

  private PostPaymentResponse handleBankResponse(
      ClientAuthorizationResponse clientAuthorizationResponse, PaymentRequest paymentRequest,
      UUID idempotencyKey) {

    PaymentStatus paymentStatus =
        clientAuthorizationResponse.authorized() ? PaymentStatus.AUTHORIZED
            : PaymentStatus.DECLINED;

    UUID paymentGatewayId =  UUID.randomUUID();

    PostPaymentResponse response = buildPaymentStructure(paymentRequest, paymentGatewayId,
        paymentStatus);
    paymentsRepository.add(idempotencyKey, response);

    LOG.info("Payment processed with Status: {}, GatewayID: {}", paymentStatus, paymentGatewayId);
    return response;
  }

  private PostPaymentResponse handleBankResponseError(PaymentRequest request, UUID idempotencyKey) {
    UUID paymentGatewayId =  UUID.randomUUID();
    paymentsRepository.add(idempotencyKey, buildPaymentStructure(request, paymentGatewayId,
        PaymentStatus.DECLINED));
    return buildPaymentStructure(request, paymentGatewayId,
        PaymentStatus.REJECTED);
  }

  private PostPaymentResponse buildPaymentStructure(PaymentRequest paymentRequest, UUID id,
      PaymentStatus paymentStatus) {

    return new PostPaymentResponse(id, paymentStatus,
        creditCardHiderService.hide(paymentRequest.cardNumber()), paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(), paymentRequest.currency(), paymentRequest.amount());
  }
}
