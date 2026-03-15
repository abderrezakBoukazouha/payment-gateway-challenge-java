package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record BankAuthorizationResponse(Boolean authorized,
                                        @JsonProperty("authorization_code") String authorizationCode) implements
    Serializable {

}
