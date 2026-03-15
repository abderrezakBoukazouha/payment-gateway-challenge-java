package com.checkout.payment.gateway.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.validation.annotations.ValidExpirationDate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class PaymentRequestValidationTest {

  private static final Validator validator;

  static {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @ParameterizedTest(name = "valid row [{index}]")
  @CsvFileSource(resources = "/payment/valid-payment-data.csv", numLinesToSkip = 1)
  void givenValidPaymentDataThenDataIsValidated(
      String firstName,
      String lastName,
      String cardNumber,
      int expiryMonth,
      int expiryYear,
      String currency,
      int amount,
      int cvv) {

    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear,
        currency, amount, cvv);
    Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

    assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
  }

  @ParameterizedTest(name = "invalid-prop row [{index}] expects={6}")
  @CsvFileSource(resources = "/payment/invalid-payment-properties-data.csv", numLinesToSkip = 1)
  void givenInvalidPaymentDataThenValidationShouldFail(String cardNumber,
      int expiryMonth,
      int expiryYear,
      String currency,
      int amount,
      int cvv,
      String expectedProperty) {

    PaymentRequest paymentRequest = new PaymentRequest(cardNumber, expiryMonth, expiryYear,
        currency, amount, cvv);
    Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

    boolean propertyFound = violations.stream()
        .anyMatch(v -> expectedProperty.equals(v.getPropertyPath().toString()));

    assertTrue(propertyFound,
        "Expected violation on property '" + expectedProperty + "', but got: " + violations);
  }

  @ParameterizedTest(name = "invalid-class row [{index}]")
  @CsvFileSource(resources = "/payment/invalid-expiration-card-date-data.csv", numLinesToSkip = 1)
  void givenInvalidExpirationDateThenValidationShouldFail(String cardNumber,
      int expiryMonth,
      int expiryYear,
      String currency,
      int amount,
      int cvv) {

    PaymentRequest req = new PaymentRequest(cardNumber, expiryMonth, expiryYear, currency, amount,
        cvv);
    Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

    boolean found = violations.stream()
        .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
        .anyMatch(ValidExpirationDate.class.getSimpleName()::equals);

    assertTrue(found, "Expected class-level %s violation but got: %s".formatted(
        ValidExpirationDate.class.getSimpleName(), violations));
  }

}


