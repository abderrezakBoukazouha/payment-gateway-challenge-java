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

* Application should be available here :http://localhost:8090 
* Open API can be found here : http://localhost:8090/swagger-ui/index.html

## Technical Design Decisions
### Key Implementation
* I tried to keep the workflow as simple as possible, as the challenge suggested. Two endpoints: one for payment processing (POST), second is for payment retrieval (GET)
* Since the challenge put a strong emphasis on validation, I went with a strong Java validation logic, so the payment request should be heavily validated following the challenge recommendation.
* Requesting an external resource requires handling async responses, and Java CompletableFuture handles it well, coupled with an Executor, it allows the application to fetch in a secure non-blocking way.
* If the bank returns an exception or timeout or crashes ? Handle it gracefully and return paymentResponse with rejected status.
* I took the choice to store everything for compliance, so any payment attempt can be retrieve by the end-user. 
* I took the decision to generate a gatewayID for every payment attempt, no matter the status, for compliance. Later, end-users can retrieve any payment attempt and check why it didn't work, rejected payment are saved as declined payment since the challenge says the following : "Status	Must be one of the following values Authorized, Declined"..
* Caching is done with Idempotency key, and it's mainly here to avoid double payment as well as to accelerate info retrieval without launching a slow async process to the bank.

### Data Modeling & Validation
* **Java Records:** I chose to work exclusively with Java Records instead of standard POJOs. This ensures immutability by default, providing better thread safety and data consistency without the need for boilerplate or external libraries like Lombok.
* **Strict Validation:** Input is heavily validated at the entry point using `jakarta.validation` annotations and custom annotations (please see the rules package).
  * **Custom Constraints:** Implemented custom validators for credit card specifics (e.g.: expiry date logic restricted to 10 years in the future), I chose to not implement the Luhn algorithm since there were no explicit requirements.
  * **Security:** Card numbers and CVVs are processed but never stored or logged in full. Only the last four digits of the PAN are retained for the response.
* **Idempotency:** I enforced a required `X-Idempotency-Key` (UUID) header in the POST endpoint. This shifts the responsibility of retry-safety to the client, preventing duplicate charges during network stutters. I took the decision to save all operations on the local cache to prevent unnecessary calls to the acquiring bank.

### Data Persistence & Caching
The repository layer utilizes two distinct `ConcurrentHashMap`s
1.  **Idempotency Cache:** Stores the mapping of `Idempotency Key -> PostPaymentResponse`. This allows for immediate returns on retried requests without re-triggering the acquiring bank. All transactions that pass validation are saved on the local cache.
2.  **Gateway Storage:** Stores All payments indexed by a unique `GatewayPaymentId`. This allows merchants to retrieve the status of past transactions. All transactions are either Authorized or Declined; Rejected transactions are saved as Declined and have their own gatewayAPI Id. All transactions that reach the acquiring bank have a gatewayAPI Id and can be retrieved.

### Asynchronous Processing
Since the `RestTemplate` is blocking, I implemented a non-blocking service layer using the **CompletableFuture API** paired with a dedicated `ThreadPoolExecutor`.
* This architecture prevents the main Servlet threads from blocking on external I/O (the bank simulator).
* It allows the system to handle higher throughput and provides a functional style for handling success and failure states gracefully, please take a look at the CompletableFuture.supplyAsync -> thenApply -> exceptionally pattern.

### Exception Handling
I complemented an already existing `@ControllerAdvice` to provide a unified, predictable error response format. This ensures that whether a failure is a validation error (400) or a bank communication issue (503/504), the client receives a structured JSON response with a clear reason and timestamp.

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


