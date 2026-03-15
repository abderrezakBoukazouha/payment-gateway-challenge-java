package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record PostPaymentRequest(
    @JsonProperty("card_number_last_four")
    int cardNumberLastFour,
    @JsonProperty("expiry_month")
    int expiryMonth,
    @JsonProperty("expiry_year")
    int expiryYear,
    String currency,
    int amount,
    int cvv) implements Serializable {


  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }
}
