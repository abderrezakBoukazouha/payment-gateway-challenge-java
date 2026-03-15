package com.checkout.payment.gateway.validation.rules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class CreditCardRuleTest {

  CreditCardRule creditCardRule = new CreditCardRule();

  @ParameterizedTest
  @CsvFileSource(resources = "/payment/valid-payment-properties.csv", numLinesToSkip = 1)
  void givenValidCreditCard_thenValidateCreditCard(String firstName, String lastName,
      String creditCardNumber) {
    Assertions.assertTrue(creditCardRule.isValid(creditCardNumber, null));
  }

}
