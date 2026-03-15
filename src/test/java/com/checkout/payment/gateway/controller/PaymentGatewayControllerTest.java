package com.checkout.payment.gateway.controller;


import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.config.BaseConfig;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


class PaymentGatewayControllerTest extends BaseConfig {

  private final String basePath = "/api/v1";
  @SpyBean
  PaymentsRepository paymentsRepository;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private ObjectMapper mapper;

  @ParameterizedTest(name = "Test {index}: Payment for {0} {1} - Status: {8}")
  @CsvFileSource(resources = "/payment/valid-payment-data.csv", numLinesToSkip = 1)
  void whenProcessPaymentWithValidRequestThenPaymentIsProcessed(String firstName, String lastName,
      String cardNumber, int expiryMonth, int expiryYear, String currency, int amount, int cvv,
      String status) {

    // GIVEN
    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear,
        currency, amount, cvv);

    HttpHeaders headers = buildHttpHeaders();

    HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

    // WHEN
    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        basePath + "/payments",
        requestEntity, PostPaymentResponse.class);

    // THEN
    assertThat(response.getBody()).isNotNull();

    PostPaymentResponse body = response.getBody();
    assertThat(body.status()).isEqualTo(PaymentStatus.valueOf(status));
    assertThat(body.amount()).isEqualTo(amount);
    assertThat(body.currency()).isEqualTo(currency);

    // Verify return ID
    if (PaymentStatus.AUTHORIZED.equals(body.status())) {
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(body.id()).isNotNull();
    }
    if (PaymentStatus.DECLINED.equals(body.status())) {
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
      assertThat(body.id()).isNull();
    }
  }

  @ParameterizedTest(name = "Test {index}: Payment for {0} {1} - Status: {8}")
  @CsvFileSource(resources = "/payment/rejected-payment-data.csv", numLinesToSkip = 1)
  void whenProcessPaymentWithInValidRequestThenPaymentIsRejected(String firstName, String lastName,
      String cardNumber, int expiryMonth, int expiryYear, String currency, int amount, int cvv,
      String status) {

    // GIVEN
    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear,
        currency, amount, cvv);

    HttpHeaders headers = buildHttpHeaders();

    HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

    // WHEN
    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        basePath + "/payments",
        requestEntity, PostPaymentResponse.class);

    // THEN
    PostPaymentResponse body = response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(body.status()).isEqualTo(PaymentStatus.valueOf(status));
    assertThat(body.amount()).isEqualTo(amount);
    assertThat(body.currency()).isEqualTo(currency);
    assertThat(body.id()).isNull();
  }

  @ParameterizedTest(name = "Test {index}: Payment for {0} {1} - Status: {8}")
  @CsvFileSource(resources = "/payment/valid-payment-data.csv", numLinesToSkip = 1)
  void whenProcessPaymentWithoutIdempotencyKeyAndValidRequest_ThenPaymentIsRejected(
      String firstName, String lastName, String cardNumber, int expiryMonth, int expiryYear,
      String currency, int amount, int cvv, String status) {

    // GIVEN
    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear,
        currency, amount, cvv);

    HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest);

    // WHEN
    ResponseEntity<String> response = restTemplate.postForEntity(basePath + "/payments",
        requestEntity,
        String.class);

    // THEN
    // set strong validation on Headers
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Rejected");
    assertThat(response.getBody()).contains("Required header X-Idempotency-Key");
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/payment/post-payment-data.csv", numLinesToSkip = 1)
  void whenRequestingExistingPayment_ThenPaymentIsReturned(UUID id, PaymentStatus status,
      String cardNumberLastFour, int expiryMonth, int expiryYear, String currency, int amount) {

    // GIVEN
    PostPaymentResponse existingPayment = new PostPaymentResponse(id, status, cardNumberLastFour,
        expiryMonth, expiryYear, currency, amount);

    paymentsRepository.add(UUID.randomUUID(), existingPayment);

    // WHEN
    ResponseEntity<PostPaymentResponse> response = restTemplate.getForEntity(
        basePath + "/payment/%s".formatted(id), PostPaymentResponse.class);

    // THEN
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    PostPaymentResponse body = response.getBody();
    assertThat(body).isNotNull();

    assertThat(body.id()).isEqualTo(id);
    assertThat(body.status()).isEqualTo(status);
    assertThat(body.cardNumberLastFour()).isEqualTo(cardNumberLastFour);
    assertThat(body.expiryMonth()).isEqualTo(expiryMonth);
    assertThat(body.expiryYear()).isEqualTo(expiryYear);
    assertThat(body.currency()).isEqualTo(currency);
    assertThat(body.amount()).isEqualTo(amount);
  }

  @Test
  void whenRequestingNonExistentPayment_ThenReturn404NotFound() {
    // GIVEN
    UUID nonExistentId = UUID.randomUUID();

    // WHEN
    ResponseEntity<Map> response = restTemplate.getForEntity(
        basePath + "/payment/%s".formatted(nonExistentId),
        Map.class
    );

    // THEN
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("status")).isEqualTo(PaymentStatus.REJECTED.getName());
    assertThat(body.get("reason")).asString().contains("Payment ID not found : " + nonExistentId);
  }


  private HttpHeaders buildHttpHeaders() {
    String idempotencyKey = UUID.randomUUID().toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Idempotency-Key", idempotencyKey);
    return headers;
  }

}
