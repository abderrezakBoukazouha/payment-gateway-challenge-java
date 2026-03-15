package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.annotations.ValidCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record BankAuthorizationRequest(@JsonProperty("card_number") String cardNumber,
                                       @JsonProperty("expiry_date") String expiryDate,
                                       @ValidCurrency String currency, @Positive int amount,
                                       String cvv) implements Serializable {

}
