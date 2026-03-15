package com.checkout.payment.gateway.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.setConnectTimeout(Duration.ofMillis(10000))
        .setReadTimeout(Duration.ofMillis(10000)).build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public ThreadPoolExecutor executor() {
    return new ScheduledThreadPoolExecutor(10);
  }
}
