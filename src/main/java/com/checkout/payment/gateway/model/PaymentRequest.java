package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.annotations.ValidCreditCard;
import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import com.checkout.payment.gateway.validation.annotations.ValidExpirationDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

@ValidExpirationDate
public record PaymentRequest(

    @ValidCreditCard @JsonProperty("card_number")
    String cardNumber,

    @Min(1) @Max(12) @JsonProperty("expiry_month")
    int expiryMonth,

    @NotNull @JsonProperty("expiry_year")
    int expiryYear,

    @NotNull @ValidCurrency
    String currency,

    @Positive
    int amount,

    @NotBlank @Pattern(regexp = "^[0-9]{3,4}$")
    String cvv
) implements Serializable {

}