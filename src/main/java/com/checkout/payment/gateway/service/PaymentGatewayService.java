package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankAuthorizationResponse;
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

  private final BankAuthorizationService bankAuthorizationService;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      CreditCardHiderService creditCardHiderService,
      BankAuthorizationService bankAuthorizationService) {
    this.paymentsRepository = paymentsRepository;
    this.creditCardHiderService = creditCardHiderService;
    this.bankAuthorizationService = bankAuthorizationService;
  }

  public Optional<PostPaymentResponse> getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id);
  }

  public CompletableFuture<PostPaymentResponse> processPayment(PaymentRequest request, String key) {
    UUID id = UUID.fromString(key);

    // Get Payment from local database, or else request
    return getPaymentById(id).map(CompletableFuture::completedFuture).orElseGet(
        () -> bankAuthorizationService.authorizePayment(request, id)
            .thenApply(
                bankAuthorizationResponse -> handleBankResponse(bankAuthorizationResponse, request, key)));
  }

  private PostPaymentResponse handleBankResponse(
      BankAuthorizationResponse bankAuthorizationResponse,
      PaymentRequest paymentRequest, String idempotencyKey) {

    PaymentStatus paymentStatus =
        bankAuthorizationResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    PostPaymentResponse paymentResponse = buildPaymentResponse(paymentRequest, idempotencyKey,
        paymentStatus);

    // Save to DB if it was a new successful process
    if (paymentStatus == PaymentStatus.AUTHORIZED) {
      LOG.info("Payment {} authorized by bank. Saving to registry.", idempotencyKey);
      paymentsRepository.add(paymentResponse);
    }

    return paymentResponse;
  }

  private PostPaymentResponse buildPaymentResponse(PaymentRequest paymentRequest,
      String idempotencyKey, PaymentStatus paymentStatus) {

    return new PostPaymentResponse(UUID.fromString(idempotencyKey), paymentStatus,
        creditCardHiderService.hide(paymentRequest.cardNumber()), paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(), paymentRequest.currency(), paymentRequest.amount());
  }
}
