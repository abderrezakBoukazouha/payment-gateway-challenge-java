package com.checkout.payment.gateway.validation.annotations;

import com.checkout.payment.gateway.validation.rules.ExpirationDateRule;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExpirationDateRule.class)
public @interface ValidExpirationDate {
  String message() default "The card has expired";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
