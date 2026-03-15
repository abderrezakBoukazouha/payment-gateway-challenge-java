package com.checkout.payment.gateway.controller;


import static com.checkout.payment.gateway.enums.PaymentStatus.AUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.config.BaseConfig;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


class PaymentGatewayControllerTest extends BaseConfig {

  @Autowired
  PaymentsRepository paymentsRepository;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper mapper;

  @ParameterizedTest(name = "Test {index}: Payment for {0} {1} - Status: {8}")
  @CsvFileSource(resources = "/payment/valid-payment-properties.csv", numLinesToSkip = 1)
  void whenProcessPaymentWithValidRequestThenPaymentIsProcessed(
      String firstName, String lastName,
      String cardNumber, int expiryMonth, int expiryYear,
      String currency, int amount, String cvv, String status) throws Exception {

    // GIVEN
    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear, currency, amount, cvv);
    String idempotencyKey = UUID.randomUUID().toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Idempotency-Key", idempotencyKey);

    HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

    // WHEN
    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity("/v1/payment", requestEntity, PostPaymentResponse.class);

    // THEN
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    PostPaymentResponse body = response.getBody();
    assertThat(body.status()).isEqualTo(PaymentStatus.valueOf(status));
    assertThat(body.amount()).isEqualTo(amount);
    assertThat(body.currency()).isEqualTo(currency);
    assertThat(body.id()).isNotNull();
  }
}
