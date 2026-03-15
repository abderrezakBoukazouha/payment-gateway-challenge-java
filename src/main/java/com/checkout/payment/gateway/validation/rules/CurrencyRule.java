package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;

public class CurrencyRule implements ConstraintValidator<ValidCurrency, String> {

  @Override
  public boolean isValid(String currency, ConstraintValidatorContext context) {

    try {
      Currency.getInstance(currency);
      return true;
    } catch (IllegalArgumentException | NullPointerException e) {
      //TODO: handle message gracefully
      return false;
    }
  }
}
