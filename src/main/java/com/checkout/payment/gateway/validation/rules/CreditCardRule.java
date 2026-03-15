package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.validation.annotations.ValidCreditCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreditCardRule implements ConstraintValidator<ValidCreditCard, String> {

  @Override
  public boolean isValid(String creditCardInput, ConstraintValidatorContext context) {

    // improvement check credit card numbers Algorithm
    return creditCardInput.matches("^\\d{14,19}$");
  }
}
