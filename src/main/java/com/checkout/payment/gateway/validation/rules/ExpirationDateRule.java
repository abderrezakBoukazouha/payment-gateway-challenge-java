package com.checkout.payment.gateway.validation.rules;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.validation.annotations.ValidExpirationDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirationDateRule implements
    ConstraintValidator<ValidExpirationDate, PaymentRequest> {

  private static final Logger LOG = LoggerFactory.getLogger(ExpirationDateRule.class);

  @Override
  public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {

    YearMonth now = YearMonth.now();
    try {
      YearMonth expiry = YearMonth.of(request.expiryYear(), request.expiryMonth());

      YearMonth maxAllowed = now.plusYears(10);

      return (expiry.isAfter(now) || expiry.equals(now)) && !expiry.isAfter(maxAllowed);
    } catch (DateTimeException e) {
      LOG.error(
          "Error in parsing credit card %s and year %s. error:  %s".formatted(request.expiryMonth(),
              request.expiryYear(), e.getMessage()));
      return false;
    }
  }
}
