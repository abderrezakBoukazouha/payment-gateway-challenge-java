package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.AcquirerClient;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentsRepository.class);

  private final Map<UUID, PostPaymentResponse> payments = new ConcurrentHashMap<>();
  private final Map<UUID, PostPaymentResponse> idempotencyIndex = new ConcurrentHashMap<>();


  public void add(UUID idempotencyKey, PostPaymentResponse payment) {
    // save AUTHORIZED and DECLINED answers to local cache
    LOG.debug("saving payment to local cache with idempotencyKey key {}", idempotencyKey);
    idempotencyIndex.put(idempotencyKey, payment);

    if (payment.id() != null) {
      LOG.debug("saving payment to repository with payment gateway key {}", payment.id());
      payments.put(payment.id(), payment);
    }
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    LOG.debug("Retrieving payment with paymentGateway key {}", id);
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<PostPaymentResponse> getByIdempotencyKey(UUID key) {
    LOG.debug("Retrieving payment with idempotency key {}", key);
    return Optional.ofNullable(idempotencyIndex.get(key));
  }

}
