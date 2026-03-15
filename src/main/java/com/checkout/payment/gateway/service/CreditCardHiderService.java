package com.checkout.payment.gateway.service;

import org.springframework.stereotype.Service;

@Service
public class CreditCardHiderService {

  public String hide(String cardNumbers) {
    int cardLength = cardNumbers.length();
    String lastFourDigits = cardNumbers.substring(cardLength - 4);
    return "%s%s".formatted("*".repeat(cardLength - 4), lastFourDigits);
  }

}
