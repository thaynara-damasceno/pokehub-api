# Copilot instructions - pokehub-api

## Stack
- Java 21, Spring Boot 4.x, Maven. Base package: `com.thaynara.pokehub`.
- Tests: JUnit 5, Mockito, AssertJ, MockMvc. WireMock for HTTP clients.

## Architecture (Clean Architecture)
- `domain`: plain Java (records, exceptions). No Spring, JPA or Jackson annotations.
- `application`: use cases and ports (`port.in`, `port.out`). No framework annotations.
- `infrastructure`: web (controllers, DTOs, exception handler), persistence (JPA),
  pokeapi (HTTP client), security, config.
- Dependencies point inward only. Use cases depend on ports, never on adapters.
- Use cases are registered as beans in `infrastructure.config`, not with `@Service`.
- Never expose JPA entities or PokeAPI response models outside `infrastructure`;
  map them to domain objects or DTOs.

## Code style
- Constructor injection only. Prefer immutability (records, final fields).
- No business logic in controllers. Validation rules live in use cases/domain.
- Domain exceptions are mapped to HTTP status codes in a single `@RestControllerAdvice`,
  with a consistent JSON error body.
- Small classes, clear names, no unused code, no commented-out code.

## Testing (TDD)
- Write the failing test first, implement the minimum to pass, then refactor.
- Never change a test just to make it pass. Never call the real PokeAPI in tests.
- Test names describe behavior. Cover edge cases and error paths.

## Process
- Do not add dependencies without asking.
- Conventional Commits (feat, fix, test, refactor, docs, chore).