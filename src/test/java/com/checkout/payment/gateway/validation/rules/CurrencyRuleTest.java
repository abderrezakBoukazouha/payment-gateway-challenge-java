package com.checkout.payment.gateway.validation.rules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CurrencyRuleTest {

  CurrencyRule currencyRule = new CurrencyRule();

  @ParameterizedTest
  @NullAndEmptySource
  void shouldNotValidateEmptyAndNullCurrency(String currencyCode) {

    Assertions.assertFalse(currencyRule.isValid(currencyCode, null),
        "should not validate empty and null sources");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ABC", "xhY", "1SG", "QKù", "%rd", "QQQ"})
  void shouldNotValidateInexistentCurrency(String currencyCode) {

    Assertions.assertFalse(currencyRule.isValid(currencyCode, null),
        "should not validate inexistent currency");
  }

  @ParameterizedTest
  @ValueSource(strings = { "USd", "AeD ", "eur", "ChF", "GBp"})
  void shouldValidatorBeCaseSensitive(String currencyCode) {

    Assertions.assertFalse(currencyRule.isValid(currencyCode, null),
        "should not validate currency due to case sensitivity");
  }

  @ParameterizedTest
  @ValueSource(strings = { " USD", "USD ", "U SD"})
  void shouldValidatorBeSpaceSensitive(String currencyCode) {

    Assertions.assertFalse(currencyRule.isValid(currencyCode, null),
        "should not validate valid currency due to space");
  }

  @ParameterizedTest
  @ValueSource(strings = { "USD", "AED", "EUR", "CHF", "GBP"})
  void shouldValidateCommonCurrencies(String currencyCode) {

    Assertions.assertTrue(currencyRule.isValid(currencyCode, null),
        "should validate valid currencies");
  }

}
