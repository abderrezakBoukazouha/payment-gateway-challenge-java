package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.model.PaymentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpirationDateRuleTest {

  ExpirationDateRule rule = new ExpirationDateRule();


  @ParameterizedTest
  @CsvSource({"2024,12", "2019,07", "2022,3", "2026,01"})
  void whenExpirationIsInThePastThenValidationIsFalse(String expirationYear,
      String expirationMonth) {

    PaymentRequest paymentRequest = new PaymentRequest(null, Integer.parseInt(expirationMonth),
        Integer.parseInt(expirationYear), null,
        0, null);

    Assertions.assertFalse(rule.isValid(paymentRequest, null),
        "should not validate expiration date in the past");
  }

  @ParameterizedTest
  @CsvSource({"2028,12", "2029,07", "2030,3"})
  void whenExpirationIsInTheFutureThenValidate(String expirationYear,
      String expirationMonth) {

    PaymentRequest paymentRequest = new PaymentRequest(null, Integer.parseInt(expirationMonth),
        Integer.parseInt(expirationYear), null,
        0, null);

    Assertions.assertTrue(rule.isValid(paymentRequest, null),
        "should not validate expiration date in the past");
  }

  //TODO: should we validate future 30 50, 100 years ?

}
