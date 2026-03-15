package com.checkout.payment.gateway.validation.annotations;

import com.checkout.payment.gateway.validation.rules.CurrencyRule;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyRule.class)
public @interface ValidCurrency {

  String message() default "must be valid currency";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
