package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyRule implements ConstraintValidator<ValidCurrency, String> {

  private static Logger LOG = LoggerFactory.getLogger(CurrencyRule.class);

  @Override
  public boolean isValid(String currency, ConstraintValidatorContext context) {

    try {
      Currency.getInstance(currency);
      return true;
    } catch (IllegalArgumentException | NullPointerException e) {
      LOG.error("Invalid currency %s ".formatted(currency));
      return false;
    }
  }
}
