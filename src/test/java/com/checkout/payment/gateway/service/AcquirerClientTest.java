package com.checkout.payment.gateway.service;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.CLientAuthorizationRequest;
import com.checkout.payment.gateway.model.ClientAuthorizationResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AcquirerClientTest {

  private final Executor executor = Runnable::run;

  private final UUID paymentId = UUID.randomUUID();

  private final PaymentRequest request = new PaymentRequest("212130976112", 12, 2028, "USD", 100,
      123);

  private final String bankUrl = "http://localhost:8080";

  @Mock
  private RestTemplate restTemplate;

  private AcquirerClient acquirerClient;

  @BeforeEach
  void setUp() {
    acquirerClient = new AcquirerClient(restTemplate, executor, bankUrl);
  }

  @Test
  void whenAuthorizePayment_andRestCallIsSuccessful_thenReturnAuthorized() {
    // GIVEN
    String authCode = UUID.randomUUID().toString();
    ClientAuthorizationResponse mockResponse = new ClientAuthorizationResponse(true, authCode);

    String expectedUrl = bankUrl + "/payments";

    when(restTemplate.postForEntity(
        anyString(),
        any(HttpEntity.class),
        eq(ClientAuthorizationResponse.class)
    )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

    // WHEN
    CompletableFuture<ClientAuthorizationResponse> future = acquirerClient.authorizePayment(
        request, paymentId);
    ClientAuthorizationResponse result = future.join();

    // THEN
    assertNotNull(result);
    assertTrue(result.authorized());
    assertEquals(authCode, result.authorizationCode());

    verify(restTemplate).postForEntity(eq(expectedUrl), any(HttpEntity.class),
        eq(ClientAuthorizationResponse.class));
  }

  @Test
  void authorizePayment_whenRestCallFails_throwsBankCommunicationException() {
    // GIVEN
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
        eq(CLientAuthorizationRequest.class))).thenThrow(new RuntimeException("Connection Timeout"));

    // WHEN & THEN
    CompletionException exception = assertThrows(CompletionException.class, () -> {
      acquirerClient.authorizePayment(request, paymentId).join();
    });

    // The cause should be our custom exception
    assertTrue(exception.getCause() instanceof BankCommunicationException);
    assertTrue(exception.getCause().getMessage()
        .contains("Bank authorization failed for ID %s".formatted(paymentId)));
  }
}
