package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.validation.annotations.ValidExpirationDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.YearMonth;

public class ExpirationDateRule implements
    ConstraintValidator<ValidExpirationDate, PaymentRequest> {

  @Override
  public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {

    YearMonth now = YearMonth.now();
    try {
      YearMonth expiry = YearMonth.of(request.expiryYear(), request.expiryMonth());
      return expiry.isAfter(now) || expiry.equals(now);
    } catch (DateTimeException e) {
      //TODO: see if we can improve it the logs
      return false;
    }
  }
}
