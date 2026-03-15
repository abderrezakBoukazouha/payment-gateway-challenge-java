package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.annotations.ValidCreditCard;
import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import com.checkout.payment.gateway.validation.annotations.ValidExpirationDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

@ValidExpirationDate
public record PaymentRequest(

    @ValidCreditCard @JsonProperty("card_number")
    String cardNumber,

    @Min(value = 1, message = "expiry month must be at least 1")
    @Max(value = 12, message = "expiry month must be 12 or less")
    @JsonProperty("expiry_month")
    int expiryMonth,

    @Positive(message = "expiry year must be positive")
    @JsonProperty("expiry_year")
    int expiryYear,

    @NotNull @ValidCurrency
    String currency,

    @Positive(message = "amount must be positive")
    int amount,

    @Min(value = 100, message = "cvv must be at least 3 digits")
    @Max(value = 9999, message = "cvv must be 4 digits or fewer")
    @JsonProperty("cvv")
    int cvv
) implements Serializable {

}