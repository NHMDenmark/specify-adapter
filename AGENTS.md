# AGENTS.md

Guidance for coding agents working in `specify-adapter`.

## Project Snapshot

- Language/runtime: Java 21.
- Build tool: Maven Wrapper (`./mvnw`).
- Framework: Spring Boot 3.1.x (Jersey + Security + Actuator).
- Persistence/migrations: JDBI + Liquibase + PostgreSQL.
- Messaging: RabbitMQ JMS listeners/broadcasters.
- API clients: Java `HttpClient` + Jackson JSON mapping.
- OpenAPI generation runs during compile (`swagger-maven-plugin-jakarta`).

## Repository Rules Files

- Checked for Cursor rules in `.cursor/rules/` and `.cursorrules`: none found.
- Checked for Copilot instructions in `.github/copilot-instructions.md`: none found.
- Therefore, follow this file and existing repository conventions.

## Setup Notes

- Use the Maven wrapper, not system Maven.
- Local secrets are expected in `application-local.properties` (not committed).
- Docker is required for Testcontainers-based tests.
- Default app port is `8081`.

## Build / Test / Run Commands

### Core commands

- Compile only: `./mvnw -DskipTests compile`
- Full test suite: `./mvnw test`
- Package JAR: `./mvnw package`
- Run application: `./mvnw spring-boot:run`

### Run a single test (important)

- One test class: `./mvnw -Dtest=MappingServiceTest test`
- One test method: `./mvnw -Dtest=MappingServiceTest#mapAssetUsesInstitutionDefaults test`
- Another example: `./mvnw -Dtest=SpecifySyncServiceTest test`

### Useful targeted commands

- Compile test sources: `./mvnw -DskipTests test-compile`
- Run tests with stack traces: `./mvnw -Dtest=ClassName test -e`
- Skip OpenAPI generation is **not configured** as a dedicated flag here; expect compile to regenerate OpenAPI artifacts in `target/classes`.

## Test Behavior and Profiles

- `SpecifySyncServiceTest` uses Testcontainers PostgreSQL.
- Spring tests should use profile `tests` when queue services must not start.
- `QueueServiceHandler` is guarded with `@Profile("!tests")`.
- If adding new `@SpringBootTest` classes that do not need AMQP startup, prefer `@ActiveProfiles("tests")`.
- Some tests are intentionally `@Disabled` for manual/queue-dependent checks.

## Code Organization

- Base package: `dk.northtech.dassco_specify_adapter`.
- Typical layout:
  - `services/` for application/service logic.
  - `webapi/` for resource endpoints.
  - `domain/` and `domain/specify/` for data models.
  - `assets/` and `configuration/` for typed properties/config.
  - `AMQP/` for queue listeners and broadcasters.
  - `repository/` for JDBI SQL object interfaces.

## Style and Conventions

### General Java style

- Use 4-space indentation.
- Keep existing brace and wrapping style; do not mass-reformat files.
- Keep imports explicit; remove unused imports when editing touched files.
- Prefer simple, readable code over aggressive abstractions.

### Dependency injection

- Prefer constructor injection.
- Existing code uses `jakarta.inject.Inject` (follow local style where you edit).
- Avoid field injection in new code.

### Types and models

- Many domain/specify DTOs use public fields instead of getters/setters.
- Keep DTO style consistent with neighboring classes.
- Use wrapper types (`Integer`, `Long`, etc.) for nullable JSON fields.
- Use `record` for small immutable config holders when matching existing patterns.

### Naming

- Class names: PascalCase.
- Methods/fields: camelCase.
- Constants: UPPER_SNAKE_CASE (`private static final`).
- Keep names domain-specific (`Specify`, `CollectionObject`, `Asset`, etc.).

### Null/empty handling

- Be explicit about nullability when mapping external API fields.
- Guard required config values early and fail fast with clear messages.
- When adding config readers, support comments (`#`) and blank lines.

### Error handling

- Existing pattern:
  - business/mapping failures: `SpecifyAdapterException` + `AcknowledgeStatus`.
  - IO/interruption/client failures: wrap in `RuntimeException` (project convention).
- Keep thrown messages actionable (include status code/body for HTTP failures where possible).
- Do not swallow exceptions silently.

### Logging

- Use SLF4J (`LoggerFactory.getLogger(...)`).
- Prefer parameterized logs (`logger.info("id {}", id)`).
- Avoid `System.out.println` in new code.

### HTTP and JSON

- Reuse Jackson `ObjectMapper` configured with:
  - `JavaTimeModule`
  - `FAIL_ON_UNKNOWN_PROPERTIES = false`
- For Specify API calls, maintain cookie + CSRF header conventions used by current services.

## Mapping Configuration Conventions

- Mapping files are under `mappings/<institution>/`.
- Existing attachment mapping templates use `.conf` files.
- Sync defaults (Specify -> ARS) use `.sync-defaults.conf` files.
- Required sync default keys:
  - `pipeline`
  - `status`
  - `workstation`
- Resolution order for sync defaults:
  - collection-specific `<collection>.sync-defaults.conf`
  - fallback `default.sync-defaults.conf`
- Missing institution default sync defaults should be treated as an error.

## Database and Liquibase

- Liquibase changelog root: `classpath:/liquibase/changelog-master.xml`.
- Integration tests may initialize schema using Liquibase against Testcontainers PostgreSQL.
- Avoid schema changes without corresponding Liquibase updates.

## Security and Config Hygiene

- Never commit secrets from `application-local.properties`.
- Keep env-var-backed properties in `application.properties` style.
- Avoid hardcoding credentials, tokens, or host-specific paths in Java code.

## Agent Workflow Recommendations

- Before editing, inspect nearby classes for style parity.
- Keep diffs minimal and task-focused; avoid opportunistic refactors.
- Run targeted tests for changed area first, then broader checks if needed.
- If adding Spring tests, consider profile impact (`tests` profile for queue isolation).
- If changing mappings/config parsing, add or update unit tests in `MappingServiceTest`.

## Quick Pre-PR Checklist

- Code compiles: `./mvnw -DskipTests compile`
- Relevant tests pass (at least targeted class/method).
- No accidental secret/config leakage.
- New config keys/files documented in this file or adjacent docs when needed.
