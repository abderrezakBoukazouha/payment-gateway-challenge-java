package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ClientAuthorizationResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  private final String idempotencyKey = UUID.randomUUID().toString();
  private final UUID id = UUID.fromString(idempotencyKey);
  private final PaymentRequest request = new PaymentRequest(
      "212130976112", 12, 2028, "USD", 100, 123);
  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private AcquirerClient acquirerClient;
  @Spy
  private CreditCardHiderService creditCardHiderService = new CreditCardHiderService();
  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Test
  void whenProcessPayment_andBankAuthorizes_thenSavePaymentAndReturnAuthorized() {
    // GIVEN
    String authCode = UUID.randomUUID().toString();
    ClientAuthorizationResponse clientAuthorizationResponse = new ClientAuthorizationResponse(true, authCode);

    when(paymentsRepository.get(id)).thenReturn(Optional.empty());
    when(acquirerClient.authorizePayment(request, id))
        .thenReturn(CompletableFuture.completedFuture(clientAuthorizationResponse));

    // WHEN
    PostPaymentResponse result = paymentGatewayService.processPayment(request, idempotencyKey)
        .join();

    // THEN
    assertEquals(PaymentStatus.AUTHORIZED, result.status());

    // Verify only once for saving and requesting bank authorization
    verify(paymentsRepository, times(1)).add(any(UUID.class),any(PostPaymentResponse.class));
    verify(acquirerClient, times(1)).authorizePayment(request, id);
    verify(creditCardHiderService, times(1)).hide(request.cardNumber());
  }

  @Test
  void whenProcessPayment_andBankDeclines_thenDoNotSaveAndReturnDeclined() {
    // GIVEN
    ClientAuthorizationResponse bankResponse = new ClientAuthorizationResponse(false, "");

    when(paymentsRepository.get(id)).thenReturn(Optional.empty());
    when(acquirerClient.authorizePayment(request, id))
        .thenReturn(CompletableFuture.completedFuture(bankResponse));

    // WHEN
    PostPaymentResponse result = paymentGatewayService.processPayment(request, idempotencyKey)
        .join();

    // THEN
    assertEquals(PaymentStatus.DECLINED, result.status());
    assertNull(result.id());
    verify(paymentsRepository, times(1)).add(any(UUID.class), any());
    verify(creditCardHiderService).hide(request.cardNumber());
  }

  @Test
  void whenProcessPayment_andBankThrowsException_thenReturnRejected() {
    // GIVEN
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());
    when(acquirerClient.authorizePayment(request, id))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Bank connection failed")));

    // WHEN
    PostPaymentResponse result = paymentGatewayService.processPayment(request, idempotencyKey)
        .join();

    // THEN
    assertEquals(PaymentStatus.REJECTED, result.status());
    verify(creditCardHiderService).hide(request.cardNumber());
    verify(paymentsRepository, never()).add(any(UUID.class), any());
  }

  @Test
  void whenProcessPayment_andPaymentAlreadyExists_thenReturnExistingPaymentWithoutBankCall() {
    // GIVEN
    PostPaymentResponse existingPayment = new PostPaymentResponse(
        id,
        PaymentStatus.AUTHORIZED,
        "************5678",
        12, 2028, "USD", 100);

    when(paymentsRepository.get(id)).thenReturn(Optional.of(existingPayment));

    // WHEN
    PostPaymentResponse result = paymentGatewayService.processPayment(request, idempotencyKey)
        .join();

    // THEN
    assertEquals(existingPayment, result);
    assertEquals(PaymentStatus.AUTHORIZED, result.status());

    // Verify that any service was never called
    verify(acquirerClient, never()).authorizePayment(any(), any());

    verify(paymentsRepository, never()).add(any(UUID.class), any());

    verify(creditCardHiderService, never()).hide(any());
  }
}
