package com.checkout.payment.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseConfig {

  private static final Logger LOG = LoggerFactory.getLogger(BaseConfig.class);

  static final GenericContainer<?> montobank;

  static {
    montobank = new GenericContainer<>(DockerImageName.parse("bbyars/mountebank:2.8.1"))
        // Map the ports defined in your compose
        .withExposedPorts(2525, 8080)

        // Map your local resource file to the path expected by the command
        .withCopyFileToContainer(MountableFile.forClasspathResource("config/bank_simulator.ejs"),
            "/imposters/bank_simulator.ejs")
        .withLogConsumer(new Slf4jLogConsumer(LOG))
        // Set the command exactly as in your docker-compose
        .withCommand("--configfile /imposters/bank_simulator.ejs --allowInjection --loglevel debug");

    montobank.start();

    LOG.info("montobank manager port : {} , server Port : {}", montobank.getMappedPort(2525), montobank.getMappedPort(8080) );

  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // This maps your app's "bank.api.url" to the dynamic Testcontainers port
    registry.add("bank.api.url",
        () -> "http://%s:%s".formatted(montobank.getHost(), montobank.getMappedPort(8080)));
  }

}
