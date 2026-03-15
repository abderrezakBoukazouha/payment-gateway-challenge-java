package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.validation.annotations.ValidCreditCard;
import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreditCardRule implements ConstraintValidator<ValidCreditCard, String> {

  @Override
  public boolean isValid(String creditCardInput, ConstraintValidatorContext context) {

    // TODO: check credit card length
    // TODO: check credit card numbers Algorithm
    return true;
  }
}
