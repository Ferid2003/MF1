# Testing strategy

## Approach

A test pyramid, not a 100%-coverage target. Most of the safety comes from fast, cheap tests; the slow ones cover only what the cheap ones cannot see.

| Level | Tool | What it proves | Where | Count today |
|---|---|---|---|---|
| Unit | JUnit 5 + Mockito | business rules of one class in isolation | `song_service` `SongServiceTest` | 3 (1 green, 2 red) |
| Integration | `@SpringBootTest` + Testcontainers (Postgres, RabbitMQ) | the layers of one service work with real infrastructure | `song_service` `SongApiIntegrationTest`; `resource_processor` `RabbitMQConsumerIntegrationTest` | 3 + 1 |
| Component | Cucumber (`songs.feature`) | one service, started on a random port, behaves as the business describes | `song_service` | 4 scenarios |
| Contract | Spring Cloud Contract (YAML contracts) | two services agree on the HTTP and message shapes they exchange | see below | 2 + 3 + 1 |
| End-to-end | Cucumber (`music_library.feature`) | the whole stack works through the gateway | `e2e_tests` | 2 scenarios |

Why this mix:

- **Unit tests are deliberately few.** The services are thin (validation + CRUD + glue). A handful of green/red-path tests cover the rules without duplicating what integration tests already exercise.
- **Integration tests carry most of the weight** because the risk lives in the wiring: JPA mappings, validation, exception handling, the exchange/queue/listener binding.
- **Component tests** state the behaviour in business language, so they double as documentation.
- **Contract tests replace most cross-service testing.** The services share no DTO classes (`SongCreationDTO` in song_service, `SongMetadataRequestDTO` in resource_processor) and the upload message is a bare `String`, so drift is silent until runtime. Contracts pin both seams without starting the other service.
- **E2E is small on purpose** (one happy path through upload → async processing → delete, one rejection). It is slow, needs the whole compose stack, and is the only place the Eureka routing, gateway and S3 are exercised together.

`gateway` and `eureka_service` contain only configuration, so they have no tests of their own; the e2e run covers them.

## Contracts and stub propagation

| Contract | Style | Provider (verifies) | Consumer (uses stubs) |
|---|---|---|---|
| `POST /songs` → 200 / 409 | HTTP | `song_service` (generated `ContractVerifierTest`, base class `ContractBase`) | `resource_processor` `SongServiceClientContractTest` |
| `resource_uploaded` | messaging | `resource_service` (generated `ContractVerifierTest`) | `resource_processor` `ResourceUploadedContractTest` |

Propagation: the Spring Cloud Contract Maven plugin builds a `*-stubs.jar` for each provider during `mvn clean install`. The consumer tests resolve it from the local Maven repository (`stubsMode = LOCAL`), so **providers must be installed before the consumer's tests run**:

```
(cd song_service && mvn clean install) && (cd resource_service && mvn clean install) && (cd resource_processor && mvn test)
```

Known limitation: Spring Cloud Contract 5.0.x has no AMQP verifier. The messaging contract is therefore verified against a channel standing in for the exchange: the provider test hands the producer a stand-in `RabbitTemplate` that forwards to the channel, so the payload, routing key and content type are checked; the consumer test delivers the stub message into the real `RabbitMQConsumer`. The real broker path (exchange → queue → listener) is covered separately by `RabbitMQConsumerIntegrationTest`.

## How to run

Docker must be running (Testcontainers).

| Type | Command |
|---|---|
| Unit, integration, component, provider contracts | `cd song_service && mvn clean install` |
| Messaging provider contract | `cd resource_service && mvn clean install` |
| Consumer contracts + RabbitMQ integration | `cd resource_processor && mvn test` (after the two installs above) |
| E2E | `docker compose up -d --build`, then `cd e2e_tests && mvn test` (`-De2e.base-url=...` to change the gateway URL) |
