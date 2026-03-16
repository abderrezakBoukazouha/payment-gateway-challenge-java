# Payment Gateway Challenge (Java)

This is a humble contribution of the Checkout.com Payment Gateway challenge. The project focuses on functionality, and clean architectural patterns.

## Prerequisites
* **JDK 17+**
* **Docker & Docker Compose** (for integration tests and running the environment)

## Entry point
At root: 
* for build : `./gradlew clean build`
* for test : `./gradlew clean test`
* for running the application : ` ./gradlew bootRun`

please ensure mountebank is running before launching the application. 
## Technical Design Decisions

### Data Modeling & Validation
* **Java Records:** I chose to work exclusively with Java Records instead of standard POJOs. This ensures immutability by default, providing better thread safety and data consistency without the need for boilerplate or external libraries like Lombok.
* **Strict Validation:** Input is heavily validated at the entry point using `jakarta.validation` annotations and custom annoations (please see the rules package)
  * **Custom Constraints:** Implemented custom validators for credit card specifics (e.g : expiry date logic restricted to 10 years in the future), i choose to not implement the Luhn algorithm since there were no explicit requirement
  * **Security:** Card numbers and CVVs are processed but never stored or logged in full. Only the last four digits of the PAN are retained for the response.
* **Idempotency:** I enforced a required `X-Idempotency-Key` (UUID) header in the Post endpoint. this shifts the responsibility of retry-safety to the client, preventing duplicate charges during network stutters, I took the decision to save All Operations on the local cache to prevent unnecessary call to the acquiring bank.

### Data Persistence & Caching
The repository layer utilizes two distinct `ConcurrentHashMap`
1.  **Idempotency Cache:** Stores the mapping of `Idempotency Key -> PostPaymentResponse`. This allows for immediate returns on retried requests without re-triggering the acquiring bank, All transactions that passes validation are saved on the local cache. 
2.  **Gateway Storage:** Stores authorized and declined payments indexed by a unique `GatewayPaymentId`. This allows merchants to retrieve the status of past transactions, All transaction are either Authorized or Declined; Rejected transaction are saved as Declined and have their own gatewayAPI Id. All transaction that reach the acquiring bank have gatewayAPI id and can be retrieved.  

### Asynchronous Processing
Since the `RestTemplate` is blocking, I implemented a non-blocking service layer using the **CompletableFuture API** paired with a dedicated `ThreadPoolExecutor`.
* This architecture prevents the main Servlet threads from blocking on external I/O (the bank simulator).
* It allows the system to handle higher throughput and provides a functional style for handling success and failure states gracefully, please take a look at the CompletableFuture.supplyAsync -> thenApply -> exceptionally pattern.

### Exception Handling
I complemented an already existing `@ControllerAdvice` to provide a unified, predictable error response format. This ensures that whether a failure is a validation error (400) or a bank communication issue (503/504), the client receives a structured JSON response with a clear reason and timestamp.

---

## Testing Strategy
I followed a "Test Pyramid" approach focused on reliability and realism:
* **Unit Testing:** Focused on validation logic, card masking, and business rules using CSV-parameterized tests to cover a wide array of edge cases.
* **Integration Testing:** Utilized **Testcontainers** to spin up a **Mountebank** instance. This allows the integration tests to hit a real network port and verify the full HTTP request/response lifecycle in an environment as close as possible to deployed environment.

---

---

## Future Improvements & Scalability


To keep things simple and avoid over-engineering for this challenge, I stuck to the core requirements. For a production-ready system, I would work on improving on:

* **Redis**: Moving the in-memory maps to a distributed cache so the application stays stateless and can scale horizontally.

* **Spring retry** To automatically reinvoke failed operation due to network or failed access. access to the acquiring bank in our example would seem helpful. 
* **Resilience4j**: Adding circuit breakers and rate limiting to the bank integration to handle external downtime and prevent traffic spikes, Although lately these concerns are solved more of  infrastructure layer than on the application layer (Kubernetes api gateway for exemeple) 

* **Java 21**: Switching to Virtual Threads to handle massive concurrency access without the overhead of managing thread pools and executors.

* **Atomic Cache Operations**: Ensuring cache access is atomic (using computeIfAbsent) to eliminate any potential race conditions on retried keys.

* **Security**: Adding Spring Security for merchant authentication. 

* **LOG4J2** : custom framework for logging

* **Observability** : With Industry standards: OpenTelemetry and LGTM stack


