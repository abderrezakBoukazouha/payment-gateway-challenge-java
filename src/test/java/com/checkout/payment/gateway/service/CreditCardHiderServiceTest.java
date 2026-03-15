package com.checkout.payment.gateway.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class CreditCardHiderServiceTest {

  static final String CARD_HIDING_REGEX_VALIDATION = "^\\*{10,15}\\d{4}$";
  final CreditCardHiderService service = new CreditCardHiderService();

  @ParameterizedTest(name = "invalid-class row [{index}]")
  @CsvFileSource(resources = "/payment/valid-payment-properties.csv", numLinesToSkip = 1)
  void whenHidingCreditCardNumberThenHideAllExceptLastFour(String cardNumber) {

    Assertions.assertTrue(service.hide(cardNumber).matches(CARD_HIDING_REGEX_VALIDATION));
  }

}
