package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.model.PaymentRequest;
import java.time.Year;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpirationDateRuleTest {

  ExpirationDateRule rule = new ExpirationDateRule();


  @ParameterizedTest
  @CsvSource({"1,12", "2,07", "3,3", "5,01"})
  void whenExpirationIsInThePast_ThenDoNotValidate(int minusExpirationYear,
      int expirationMonth) {

    String expirationYear = Year.now().minusYears(minusExpirationYear).toString();
    PaymentRequest paymentRequest = new PaymentRequest(null, expirationMonth,
        Integer.parseInt(expirationYear), null,
        0, 100);

    Assertions.assertFalse(rule.isValid(paymentRequest, null),
        "should not validate expiration date in the past");
  }

  @ParameterizedTest
  @CsvSource({"1,12", "5,07", "7,3"})
  void whenExpirationIsInTheFuture_ThenValidate(int plusExpirationYear,
      String expirationMonth) {

    String expirationYear = Year.now().plusYears(plusExpirationYear).toString();

    PaymentRequest paymentRequest = new PaymentRequest(null, Integer.parseInt(expirationMonth),
        Integer.parseInt(expirationYear), null,
        0, 100);

    Assertions.assertTrue(rule.isValid(paymentRequest, null),
        "should not validate expiration date in the past");
  }

  @ParameterizedTest
  @CsvSource({"15,12", "25,07"})
  void whenExpirationIsInTheFarFuture_ThenDoNotValidate(int plusExpirationYear,
      String expirationMonth) {

    String expirationYear = Year.now().plusYears(plusExpirationYear).toString();
    PaymentRequest paymentRequest = new PaymentRequest(null, Integer.parseInt(expirationMonth),
        Integer.parseInt(expirationYear), null,
        0, 100);

    Assertions.assertFalse(rule.isValid(paymentRequest, null),
        "should not validate expiration date in the far future");
  }
}
