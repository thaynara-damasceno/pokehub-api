# GenAI usage - pokehub-api

This document records how I used GenAI tools while building this project: what I asked,
what I got back, how I validated it, and what I changed. It also covers the
task-management exercise requested in the technical interview.

## 1. Principles

- AI is a pair programmer; **I own the architecture and the decisions**.
- I define the rules first (Clean Architecture, TDD) and give them to the tool as
  project-level instructions (`.github/copilot-instructions.md`).
- Prompts state the exact contract (class names, fields, rules, what NOT to do).
- I never accept generated code without reading the diff and running the tests.
- Every relevant interaction is logged below (section 4).

## 2. Tools

| Tool | What I used it for |
|---|---|
| GitHub Copilot (IntelliJ) | Code generation: models, adapters, mappers, test scaffolding |
| Claude (chat assistant) | Planning the steps, drafting and reviewing prompts, reviewing diffs and explaining errors. Snippets it suggested were reviewed before use; code generation in the repository was done with Copilot |

## 3. How I validate AI-generated code

1. **Architecture**: no Spring/JPA/Jackson imports in `domain` or `application`;
   dependencies point inward; JPA entities never reach controllers.
2. **Tests**: written first (TDD); they assert real behavior; the AI must not edit
   tests to make them pass; red -> green is visible in the commit history.
3. **Run it**: `./mvnw clean verify` passes; the app starts against Postgres.
4. **Edge cases**: invalid input, missing records, PokeAPI unavailable or slow.
5. **Security**: no secrets in code, passwords hashed, protected vs public routes checked.
6. **Readability**: naming, small classes, no dead code.

## 4. Prompt log

Template for each entry:

- **Step / goal:**
- **Prompt used:** (exact text)
- **What the AI generated:**
- **What I checked:**
- **Problems found:**
- **What I changed / fixed:**
- **Lesson learned:**

### Entry 01 - Story 01, Part 1A: domain, ports and failing tests

- **Goal:** create the `Pokemon` and `PokemonPage` records, the pagination exception, the
  `PokemonGateway` and `ListPokemonUseCase` ports, a `ListPokemonService` skeleton, and the
  unit tests first (TDD red phase).
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode, with the project rules in
  `.github/copilot-instructions.md`.
- **Prompt used:**

```text
We are building a Spring Boot REST API (package com.thaynara.pokehub) following Clean Architecture and TDD.
This is the first step of "User Story 01: list Pokémon with pagination". Do NOT use any Spring, JPA or
Jackson annotation in the domain or application packages.

Create these files:

1. domain/model/Pokemon.java - a Java record: int id, String name, String spriteUrl,
   List<String> types, int weight, List<String> abilities.
2. domain/model/PokemonPage.java - a record: List<Pokemon> items, int page, int size, long totalElements,
   with a method int totalPages() that returns ceil(totalElements / size) (0 when size is 0).
3. domain/exception/InvalidPaginationException.java - a RuntimeException with a String message constructor.
4. application/port/out/PokemonGateway.java - interface with: PokemonPage findPage(int page, int size);
5. application/port/in/ListPokemonUseCase.java - interface with: PokemonPage list(int page, int size);
6. application/usecase/ListPokemonService.java - implements ListPokemonUseCase, receives a PokemonGateway
   in the constructor, has a package-private constant MAX_PAGE_SIZE = 50.
   IMPORTANT: leave the list method body as `throw new UnsupportedOperationException();` (we are in TDD,
   the tests must fail first). No framework annotations on this class.
7. src/test/.../application/usecase/ListPokemonServiceTest.java - JUnit 5 + Mockito + AssertJ,
   with @ExtendWith(MockitoExtension.class). Test cases:
   - returns the page provided by the gateway (mock gateway.findPage(0, 20), assert the same page is
     returned and the gateway was called once with 0 and 20);
   - rejects a negative page with InvalidPaginationException and never calls the gateway;
   - rejects size 0, -5 and 51 (parameterized test) with InvalidPaginationException and never calls
     the gateway;
   - accepts the boundaries: size 1 and size 50 (delegates to the gateway).
Use clear test names that describe the behavior.
```

- **Control over the agent:** the agent asked for permission before running commands. I read
  each command (for example a read-only `git status` and `ls` to explore the project) and
  approved them one at a time ("Allow Once"). I did not grant permanent access to `git` or to
  all commands, so it could not commit, push or delete anything on its own.
- **What I checked:** no Spring/JPA/Jackson imports in `domain` or `application`; no
  annotations on `ListPokemonService`; the `list` method still throws
  `UnsupportedOperationException`; tests fail for the right reason and not because of a
  compilation error; the gateway is never called when the input is invalid.
- **What the AI generated:** all seven requested files: the `Pokemon` and `PokemonPage` records,
  `InvalidPaginationException`, the `PokemonGateway` and `ListPokemonUseCase` ports, the
  `ListPokemonService` skeleton (with `MAX_PAGE_SIZE = 50`, framework-free, `list` throwing
  `UnsupportedOperationException`) and `ListPokemonServiceTest` with 7 test executions
  (happy path, negative page, three invalid sizes, two boundary sizes). The agent also ran the
  tests and reported the expected failure.
- **Problems found:** none in this first pass. The output matched the contract in the prompt.
  I did not rely on the agent's own summary: I confirmed it by running the tests myself (7 failures,
  all caused by `UnsupportedOperationException`) and by grepping `domain` and `application` for
  Spring, Jakarta and Jackson imports (no results).
- **What I changed / fixed:** nothing; I accepted the generated code after reviewing it.
- **Lesson learned:** stating the exact contract in the prompt (class names, fields, rules)
  and the TDD constraint ("leave the method throwing, tests must fail first") kept the agent from
  implementing ahead of the tests. Even when the first result is correct, I verify it
  independently instead of trusting the tool's summary.

### Entry 02 - Story 01, Part 1B: pagination rules (TDD green)

- **Goal:** implement the minimum in `ListPokemonService.list` to make the failing tests pass,
  without touching the tests.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode.
- **Prompt used:**

```text
Implement ListPokemonService.list(int page, int size) so that all tests in ListPokemonServiceTest pass.
Rules: page must be >= 0, size must be between 1 and MAX_PAGE_SIZE (inclusive); otherwise throw
InvalidPaginationException with a clear message, without calling the gateway. Otherwise delegate to
gateway.findPage(page, size). Do the minimum needed to make the tests pass and do not change the tests.
```

- **What the AI generated:** the validation in `ListPokemonService.list`: a negative page or a size
  outside `1..MAX_PAGE_SIZE` throws `InvalidPaginationException` before the gateway is called;
  valid requests are delegated to `gateway.findPage(page, size)`.
- **What I checked:** `git diff --stat` showed only `ListPokemonService.java` modified (the tests
  were untouched); the class still has no framework annotations; I ran `./mvnw clean verify`
  myself and all `ListPokemonServiceTest` cases passed (red -> green).
- **Problems found:** none. The agent did the minimum requested and respected the instruction not to
  change the tests.
- **What I changed / fixed:** nothing.
- **Lesson learned:** the explicit constraint "do the minimum and do not change the tests" is what
  keeps the TDD cycle honest. The red commit followed by the green commit is visible in the Git
  history, so the work can be verified instead of just claimed.

### Entry 03 - Story 01, Part 2A: PokeAPI adapter tests (TDD red) and a design gap

- **Goal:** create the `PokemonProviderUnavailableException`, a `PokeApiPokemonGateway` skeleton and
  the WireMock tests first (TDD red phase) for the adapter that implements the `PokemonGateway` port.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **First prompt used:**

```text
Step 2A of "User Story 01: list Pokémon with pagination" (TDD red phase). We are creating the PokeAPI
adapter that implements the existing PokemonGateway port. Follow .github/copilot-instructions.md. Do NOT add
or change any dependency in pom.xml (WireMock standalone is already there as a test dependency).

Create these files:

1. domain/exception/PokemonProviderUnavailableException.java - RuntimeException with constructors
   (String message) and (String message, Throwable cause). Plain Java, no framework imports.
2. infrastructure/pokeapi/PokeApiPokemonGateway.java - implements PokemonGateway, constructor receives a
   Spring RestClient (org.springframework.web.client.RestClient). IMPORTANT: leave findPage with
   `throw new UnsupportedOperationException();` (the tests must fail first). No Spring stereotype annotations.
3. Test resources in src/test/resources/pokeapi/: trimmed but realistic PokeAPI JSON payloads:
   - pokemon-list-page0-size2.json: {"count":1350,"next":...,"previous":null,"results":[bulbasaur, ivysaur
     with name and url]}
   - pokemon-bulbasaur.json and pokemon-ivysaur.json: id, name, weight, "types" (give the entries in
     reversed slot order on purpose: slot 2 first, then slot 1), "abilities" (with is_hidden and slot),
     "sprites": {"front_default": "..."}, plus a few extra fields not used by us ("moves", "forms") to prove
     unknown properties are ignored.
4. src/test/.../infrastructure/pokeapi/PokeApiPokemonGatewayTest.java - JUnit 5 + AssertJ, WireMock via
   WireMockExtension with a dynamic port (@RegisterExtension). No Spring context, no real network. Each test
   builds `RestClient.builder().baseUrl(wireMock.baseUrl()).build()` and the gateway. Test cases:
   - maps the list + detail responses to a PokemonPage (page 0, size 2): items in list order, all fields
     mapped, types and abilities ordered by slot, page/size/totalElements filled;
   - translates page and size to limit and offset (page 2, size 20 -> limit=20&offset=40), verified with
     WireMock request verification;
   - keeps the list order even when the first detail response is slower than the second (use a fixed delay);
   - returns an empty items list but keeps totalElements when the list "results" is empty;
   - maps a null "front_default" sprite to a null spriteUrl;
   - throws PokemonProviderUnavailableException when the list call returns HTTP 500;
   - throws PokemonProviderUnavailableException when one detail call returns HTTP 500;
   - throws PokemonProviderUnavailableException when the PokeAPI answers slower than the read timeout
     (build the RestClient in that test with a JdkClientHttpRequestFactory with a 200 ms read timeout and
     stub a 1000 ms delay).
Use clear test names that describe the behavior.
```

- **What the AI generated:** the exception, the gateway skeleton throwing `UnsupportedOperationException`,
  three PokeAPI JSON fixtures and eight WireMock tests (mapping, pagination parameters, ordering,
  empty results, null sprite, HTTP 500 on list, HTTP 500 on detail, read timeout). No dependency changes.
- **What I checked:** no Spring, Jakarta or Jackson imports in `domain` and `application`; `pom.xml`
  untouched; no test calling the real internet; the first test run was red (8 tests failing).
- **Problems found:** a design gap. My prompt did not say how the adapter builds the detail URL or what the
  RestClient base URL is, so the agent filled the gap on its own. Its tests rewrote the `url` fields of
  the fixtures to point to WireMock and stubbed the detail calls by id (`/api/v2/pokemon/1/`). That would
  have forced the adapter to follow URLs returned by an external service. I did not get this from the
  agent's summary, which said everything was fine: I found it by running a `grep` on the generated test
  (it showed a `replace(...)` of `https://pokeapi.co/api/v2`) and reviewing the result with a chat
  assistant as a second reviewer.
- **What I changed / fixed:** I decided the design myself: the adapter builds `/pokemon/{name}` from the
  name in the list and never follows URLs from the response, so the configured base URL applies to
  every call. I sent a second prompt limited to the test code:

```text
The tests in PokeApiPokemonGatewayTest assume a different design than the one we want. Fix ONLY the test code
and the JSON fixtures; do not touch any class under src/main and do not touch pom.xml.

Contract the tests must follow:
- The gateway's RestClient base URL is the API root INCLUDING /api/v2. In every test build it as
  RestClient.builder().baseUrl(wireMock.baseUrl() + "/api/v2").build() (also in the timeout test).
- The gateway calls GET /pokemon?limit={size}&offset={page*size} for the list and then
  GET /pokemon/{name} for each item, using the NAME from the list item. It never follows the "url" field of
  the list items.
- WireMock stubs: /api/v2/pokemon (list) and /api/v2/pokemon/bulbasaur, /api/v2/pokemon/ivysaur (details).
  No trailing slash, no lookup by id.
- Remove the code that rewrites the "url" fields of the fixtures to point to WireMock (the replace(...) of
  "https://pokeapi.co/api/v2" and the formatted(wireMock.baseUrl()) helper). The fixtures keep the original
  pokeapi.co URLs, which must simply be ignored.
Keep the same 8 test cases and names. The tests must still fail only because findPage is not implemented.
```

After the fix I re-ran the tests: 8 tests red, all failing with `UnsupportedOperationException`, and
I confirmed with `grep` that the stubs and the base URL follow the contract.

- **Lesson learned:** when a prompt leaves a design decision open, the agent decides for me, and its
  summary will not mention it. For anything that touches an integration boundary (base URL, how
  external URLs are used, error mapping) I now state the contract explicitly in the prompt, and I review
  the generated tests for hidden assumptions before implementing anything.

### Entry 04 - Story 01, Part 2B: PokeAPI adapter implementation (TDD green)

- **Goal:** implement `PokeApiPokemonGateway` so the eight failing tests pass, plus the Spring
  configuration that wires the `RestClient` and the `PokemonGateway` beans.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompt used:**

```text
Step 2B of "User Story 01" (TDD green phase). Make all tests in PokeApiPokemonGatewayTest pass without
changing them, following .github/copilot-instructions.md. Do not add dependencies.

Design decisions (already fixed, do not change them):
- The RestClient base URL is the API root including /api/v2 (https://pokeapi.co/api/v2 in production), so the
  gateway uses relative paths: GET /pokemon?limit={size}&offset={page*size} and GET /pokemon/{name}.
- The detail call uses the NAME from each list item. The gateway never follows the "url" field returned
  by the PokeAPI.

1. In infrastructure/pokeapi create package-private response records used only to deserialize PokeAPI JSON
   (list response, list item, pokemon detail with sprites, types and abilities). Use Jackson annotations only
   where needed (@JsonProperty for snake_case, @JsonIgnoreProperties(ignoreUnknown = true)). Note: this project
   uses Spring Boot 4 / Jackson 3, so do not import com.fasterxml.jackson.databind; the annotations package
   com.fasterxml.jackson.annotation is the correct one.
2. Implement PokeApiPokemonGateway.findPage(page, size): fetch the list, then fetch every detail
   concurrently using a virtual-thread executor (Executors.newVirtualThreadPerTaskExecutor()), keep the
   original list order, and map to the domain Pokemon / PokemonPage. Types and abilities sorted by slot
   (abilities include the hidden ones). weight is kept as returned by the PokeAPI.
3. Wrap any RestClientException in PokemonProviderUnavailableException with a clear message. If a concurrent
   detail call fails, unwrap the CompletionException so the caller receives PokemonProviderUnavailableException.
4. The response records must NOT leak out of the infrastructure.pokeapi package.
5. Create infrastructure/config/PokeApiConfig.java (@Configuration) that exposes a RestClient bean (base URL
   and timeouts from properties, JdkClientHttpRequestFactory) and a PokemonGateway bean built with it.
   Add to src/main/resources/application.yaml:
   pokeapi.base-url: https://pokeapi.co/api/v2, pokeapi.connect-timeout-ms: 2000, pokeapi.read-timeout-ms: 5000.
Do the minimum needed. Do not modify the tests.
```

- **What the AI generated:** the gateway (detail requests in parallel on virtual threads with the executor
  closed by try-with-resources, results kept in list order, types and abilities sorted by slot,
  provider errors translated to `PokemonProviderUnavailableException` with the original cause kept),
  package-private response records, `PokeApiConfig` with the `RestClient` and `PokemonGateway` beans, and
  the three `pokeapi.*` properties in `application.yaml`. The eight gateway tests passed.
- **What I checked:** `git diff --stat` showed no test and no `pom.xml` changed; `grep` found no Spring,
  Jakarta or Jackson imports in `domain` and `application`, and no `RestTemplate`, `WebClient`,
  `Thread.sleep` or `System.out`; the PokeAPI URL exists only in `application.yaml`. I reviewed the code
  against a checklist, with a chat assistant as a second reviewer: offset is `page * size`; the detail call
  uses the name as a URI variable and never follows URLs returned by the API; all futures are created before
  being joined in list order; the `CompletionException` is unwrapped; Jackson 3 packages are correct.
- **Problems found:** green tests did not mean good design. (1) Unrequested complexity: the gateway read
  the body as a `String` and parsed it with its own Jackson mapper, duplicating what `RestClient` already
  does and adding a second error path. (2) Code quality: seven top-level records in one file, and a `url`
  field in the list item that is never used. (3) Gaps not covered by tests, noted as follow-ups: `page * size`
  can overflow `int` for absurd page numbers, and null `results` or `types` would throw a
  `NullPointerException`.
- **What I changed / fixed:** the refactor was done in a separate commit with the tests as a safety net
  (see Entry 05).
- **Lesson learned:** tests prove behavior, not design. After going green I review the diff for complexity
  the agent added on its own, and I refactor with the tests protecting me.

### Entry 05 - Story 01, Part 2B: refactor and the stubs that hid a bug

- **Goal:** refactor the adapter after going green, keeping the behavior identical and the tests as a
  safety net.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompt used:**

```text
Refactor step (TDD refactor phase): keep the behavior identical and all tests green. Do not modify any
test, pom.xml, application.yaml or any file outside infrastructure/pokeapi.

1. In PokeApiPokemonGateway remove the manual Jackson usage (ObjectMapper, JsonMapper, JacksonException and
   the String body parsing). Let RestClient deserialize directly with .body(responseType). Keep wrapping
   RestClientException (it also covers conversion errors) in PokemonProviderUnavailableException.
2. Replace PokeApiResponses.java (several top-level records in one file) with a single final class
   PokeApiResponses (private constructor) that holds the records as package-private nested records, and
   update all references.
3. Remove the unused `url` component from the list item record. The gateway must never use the URLs
   returned by the PokeAPI (ignoreUnknown already discards that field).
Run ./mvnw test and report the result. If removing the manual parsing makes any test fail, stop and tell
me why instead of working around it.
```

- **What the AI generated:** the gateway without the manual Jackson parsing (the `RestClient` now
  deserializes the body into the response records) and the records grouped as nested types of a single
  `PokeApiResponses` class.
- **What I checked:** `git status` showed that the refactor touched only `PokeApiPokemonGateway.java`
  and `PokeApiResponses.java` under `infrastructure/pokeapi`, a `grep` confirming no `databind`,
  `JacksonException` or `JsonMapper` was left in `src/main/java`, and a full `./mvnw clean verify`.
- **Problems found:** the full build was red: 3 of the 8 gateway tests failed with
  `UnknownContentTypeException: no suitable HttpMessageConverter ... content type [application/octet-stream]`.
  The root cause was in the tests, not in the refactor. The WireMock stubs for the detail calls returned
  the JSON body without a `Content-Type` header, so WireMock served it as `application/octet-stream`.
  The previous implementation read every body as a `String` and parsed it by hand, ignoring the header,
  so it had been hiding the inaccurate stubs. The real PokeAPI answers with
  `application/json; charset=utf-8`, which I confirmed with `curl -I`.
- **What I changed / fixed:** I fixed it myself, by hand, instead of delegating it back to the agent, and I
  treated this as a deliberate exception to the rule "never change tests to make them pass": the tests were
  modelling the external API incorrectly. I changed only the stubs, adding `Content-Type: application/json`
  to the detail responses (the delayed stub of the ordering test and the shared `stubDetail` helper). No
  assertion, test name, fixture or production class was changed. After that, `./mvnw clean verify` passed
  with all 16 tests.
- **Review of my own change:** before committing I read the diff of my manual fix and found a mistake
  I had introduced by copying an AI-suggested snippet literally: the helper's `.withStatus(status)` had been
  replaced by a hard-coded `.withStatus(200)` (plus a leftover comment), which would have made the helper
  ignore its status parameter. I restored the original line and re-ran the full build before committing.
  Takeaway: AI-suggested snippets, including those from a chat assistant, are starting points; I always
  review the diff line by line, and I only commit after the full build is green.
- **Lesson learned:** a green suite can hide wrong test doubles. Test doubles must imitate the real service
  (status codes, headers, content type), and I verify them against the real API with `curl`. The refactor
  step also worked as designed: the tests acted as a safety net and exposed an incorrect assumption that
  the first implementation had masked.

### Entry 06 - Story 01, Part 3A: web layer tests (TDD red) and a red for the wrong reason

- **Goal:** create the web DTOs, a controller skeleton, an empty exception advice and the MockMvc tests
  first (TDD red phase) for `GET /api/pokemon`.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompt used:**

```text
Step 3A of "User Story 01: list Pokémon with pagination" (TDD red phase). We are creating the web layer.
Follow .github/copilot-instructions.md. Do NOT add or change any dependency in pom.xml, and do NOT create any
security configuration yet (security gets its own step).

Create these files in package com.thaynara.pokehub.infrastructure.web:

1. PokemonResponse.java - record: int id, String name, String spriteUrl, List<String> types, int weight,
   List<String> abilities. Fields only (no mapping method yet).
2. PokemonPageResponse.java - record: List<PokemonResponse> items, int page, int size, long totalElements,
   int totalPages. Fields only.
3. PokemonController.java - @RestController, @RequestMapping("/api/pokemon"), constructor receives the
   ListPokemonUseCase port (never ListPokemonService). One @GetMapping method
   `PokemonPageResponse list(int page, int size)` with NO @RequestParam yet. IMPORTANT: its body must be
   `throw new UnsupportedOperationException();` (the tests must fail first).
4. GlobalExceptionHandler.java - an empty @RestControllerAdvice class, with no handler methods yet.
5. src/test/.../infrastructure/web/PokemonControllerTest.java - JUnit 5 + AssertJ + MockMvc, using
   @WebMvcTest(PokemonController.class), @AutoConfigureMockMvc(addFilters = false) (security is tested
   separately) and a @MockitoBean ListPokemonUseCase. Note: this project uses Spring Boot 4, so do not use
   @MockBean (removed); use @MockitoBean (org.springframework.test.context.bean.override.mockito) and the
   Spring Boot 4 test packages (org.springframework.boot.webmvc.test.autoconfigure for @WebMvcTest and
   @AutoConfigureMockMvc). If an import does not resolve, tell me instead of adding dependencies.
   Contract the tests must verify:
   - GET /api/pokemon with no parameters calls the use case with page 0 and size 20 and answers 200;
   - GET /api/pokemon?page=2&size=10 calls the use case with 2 and 10;
   - the 200 body has: items[] with id, name, spriteUrl, types, weight, abilities, plus page, size,
     totalElements and totalPages (totalPages comes from PokemonPage.totalPages());
   - an empty page answers 200 with items = [] and totalPages = 0;
   - when the use case throws InvalidPaginationException("size must be between 1 and 50"), the answer is 400
     with content type application/problem+json, status 400 and detail equal to the exception message;
   - when the use case throws PokemonProviderUnavailableException with an internal message, the answer is
     502 with content type application/problem+json, status 502 and detail exactly
     "The Pokémon data provider is currently unavailable. Please try again later."; the internal exception
     message must NOT appear anywhere in the body;
   - GET /api/pokemon?page=abc answers 400 with content type application/problem+json and detail exactly
     "Invalid value for parameter 'page'.".
Use clear test names that describe the behavior.
```

- **What the AI generated:** the two response records, a controller skeleton that receives the
  `ListPokemonUseCase` port and throws `UnsupportedOperationException`, an empty `@RestControllerAdvice`
  and `PokemonControllerTest` with seven MockMvc tests. It used `@MockitoBean` and the Spring Boot 4 test
  packages correctly, which I had named explicitly in the prompt.
- **What I checked:** `pom.xml` untouched and only new files in `git status`; no `@MockBean` anywhere; no
  Spring, Jakarta or Jackson imports in `domain` and `application`; I read the test against the contract
  (JSON fields, `totalPages`, empty page, problem+json content type, and an assertion that the internal
  exception message does not leak in the 502 body).
- **Problems found:** the agent's summary said all seven tests were red "as expected", but four of them
  were failing for the wrong reason: `IllegalStateException: Optional int parameter 'page' is present but
  cannot be translated into a null value due to being declared as a primitive type`. The skeleton declared
  `int page, int size` without `@RequestParam`, so Spring failed while resolving the arguments, before the
  controller body was ever reached. The root cause was my prompt, which asked for "NO @RequestParam yet".
  I found it by reading the failure messages of the test run (reviewed together with a chat assistant)
  instead of trusting the summary.
- **What I changed / fixed:** I corrected the skeleton myself with a one-line change per parameter,
  `@RequestParam(defaultValue = "0") int page` and `@RequestParam(defaultValue = "20") int size`, and kept
  the `UnsupportedOperationException` body. After that, the run showed seven red tests for the right
  reasons: six `UnsupportedOperationException` and one `Content type not set` (Spring already answered
  400 for `page=abc`, but without the problem+json body that the handler will produce).
- **Improvements to the AI-generated tests:** after the fix I reviewed the generated tests and improved
  them by hand before committing. I replaced the fully-qualified Hamcrest calls with static imports and
  added an eighth test for a non-numeric `size` parameter (the agent had only covered `page`), which uses
  the same handler. The final red run showed eight failing tests for the right reasons: six
  `UnsupportedOperationException` and two `Content type not set`.
- **Lesson learned:** a red test only proves something when it fails for the right reason. A TDD skeleton
  has to be complete enough for the framework to route and bind the request, so the tests reach the
  behavior under test. I always read the failure reasons, not just the red/green count.

### Entry 07 - Story 01, Part 3B: web layer implementation (TDD green)

- **Goal:** implement the controller, the DTO mapping and the error handling so the eight
  `PokemonControllerTest` tests pass, and wire the use case as a bean.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompt used:**

```text
Step 3B of "User Story 01" (TDD green phase). Make all tests in PokemonControllerTest pass without
changing them, and keep the existing tests green (including PokehubApiApplicationTests, which loads the whole
context). Follow .github/copilot-instructions.md. Do not add dependencies, do not touch pom.xml,
application.yaml, any test, or any class under domain or application. Do NOT create any Spring Security
configuration yet.

Design decisions (already fixed, do not change them):
- Pagination rules live ONLY in the use case. The controller must not validate page or size.
- The controller keeps `@RequestParam(defaultValue = "0") int page` and `@RequestParam(defaultValue = "20")
  int size` and only delegates to the ListPokemonUseCase port.
- Mapping domain -> DTO lives in the web layer: static factory methods PokemonResponse.from(Pokemon) and
  PokemonPageResponse.from(PokemonPage) (totalPages comes from PokemonPage.totalPages()).
- Errors are returned as Spring ProblemDetail (application/problem+json) by GlobalExceptionHandler, with
  one @ExceptionHandler per case:
  * InvalidPaginationException -> 400, detail = the exception message.
  * PokemonProviderUnavailableException -> 502, detail exactly "The Pokémon data provider is currently
    unavailable. Please try again later." The internal message must NOT be returned; log the exception on
    the server with SLF4J (org.slf4j.Logger) instead.
  * MethodArgumentTypeMismatchException -> 400, detail "Invalid value for parameter '<name>'." using the
    parameter name.
- Do not use Lombok.

1. Implement the DTO factory methods and the controller body.
2. Implement GlobalExceptionHandler with the three handlers above.
3. Create infrastructure/config/UseCaseConfig.java (@Configuration) with a bean of type ListPokemonUseCase
   built as `new ListPokemonService(PokemonGateway)`. ListPokemonService must stay free of framework
   annotations.
Do the minimum needed. Run ./mvnw clean verify and report the result. If a test fails, tell me why instead
of changing the test.
```

- **What the AI generated:** the `from(...)` mapping methods on the two response records, the controller
  delegating to the `ListPokemonUseCase` port with the default `page` and `size`, a
  `GlobalExceptionHandler` with three `ProblemDetail` handlers (400 for invalid pagination, 502 for an
  unavailable provider, 400 for a parameter type mismatch) and `UseCaseConfig`, which registers
  `ListPokemonService` as a bean.
- **What I checked:** `git diff --stat` showed only the controller, the two DTOs and the handler modified,
  plus the new `UseCaseConfig`: no test, no `pom.xml` and no `application.yaml` changed; `grep` found no
  Spring, Jakarta or Jackson imports in `domain` and `application`; a `grep` for `getMessage` in the web
  package showed it only in the 400 handler. I read the 502 handler myself: it returns the fixed text and
  sends the real exception to the server log, so no internal detail reaches the client. I ran
  `./mvnw clean verify` myself: BUILD SUCCESS with 24 tests.
- **Problems found:** none. The agent's summary (24 tests passing) matched my own run, and nothing
  was added outside the contract.
- **What I changed / fixed:** nothing.
- **Lesson learned:** applying the lesson from Entry 03, I wrote the design decisions into the prompt
  (validation only in the use case, a sanitized 502 body with the real error logged, DTO mapping in the web
  layer, the use case registered as a bean without annotating the application class). With the contract
  explicit, the agent had nothing left to decide on its own, and the review reduced to verifying the diff
  and the security-sensitive handler.

### Entry 08 - Story 01, Part 3C: security rules (TDD red/green)

- **Goal:** define which routes are public and which are protected, test-first: the Pokémon listing, the
  OpenAPI docs and the Swagger UI are public, the auth endpoints will be public, and everything else requires
  authentication.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompts used:**

```text
Step 3C-a (TDD red phase): security rules for the API. Follow .github/copilot-instructions.md. Do NOT add or change
any dependency, do NOT touch pom.xml, application.yaml or any existing class or test, and do NOT create any
security configuration class yet (the tests must fail first against Spring Boot's default security).

Create src/test/java/com/thaynara/pokehub/infrastructure/security/SecurityRulesTest.java using
@SpringBootTest, @AutoConfigureMockMvc, @ActiveProfiles("test") and a @MockitoBean ListPokemonUseCase (so no
real PokeAPI call is made; make it return an empty PokemonPage). Spring Boot 4 note: @AutoConfigureMockMvc is in
org.springframework.boot.webmvc.test.autoconfigure and @MockitoBean in
org.springframework.test.context.bean.override.mockito. If an import does not resolve, tell me instead of adding dependencies.
Tests, all WITHOUT credentials:
- GET /api/pokemon answers 200;
- GET /v3/api-docs answers 200;
- GET /swagger-ui/index.html answers 200;
- POST /api/pokemon/1/sync answers 401 (this route does not exist yet; unauthenticated writes must be rejected
  before routing, and must not be rejected with 403 because of CSRF);
- GET /api/local-pokemon answers 401.
Use clear test names that describe the behavior.
```

```text
Step 3C-b (TDD green phase). Make SecurityRulesTest pass without changing any test, and keep all other tests green.
Follow .github/copilot-instructions.md. Do not add dependencies, do not touch pom.xml, application.yaml, any test, or any
class under domain, application or infrastructure/pokeapi.

Design decisions (already fixed, do not change them):
- Create infrastructure/security/SecurityConfig.java (@Configuration) exposing a SecurityFilterChain bean.
- The API is stateless: disable CSRF and use session creation policy STATELESS.
- Public routes: GET /api/pokemon/**, GET /v3/api-docs/**, GET /swagger-ui/**, GET /swagger-ui.html, and /api/auth/**
  (any method; the auth endpoints will be created later).
- Every other request requires authentication. Keep httpBasic() enabled for now as a placeholder (it will be
  replaced when user registration and login are implemented); unauthenticated requests must answer 401.
- Do not create users, password encoders or JWT classes in this step.
Do the minimum needed. Run ./mvnw clean verify and report the result. If a test fails, tell me why instead of changing the test.
```

- **What the AI generated:** `SecurityRulesTest` with five MockMvc tests, and `SecurityConfig` with a stateless
  `SecurityFilterChain` (CSRF disabled, the listed public routes, authentication required for everything
  else, HTTP Basic as a placeholder).
- **What I checked:** in the red run, four tests failed for the right reasons: the three public `GET`s
  answered 401 instead of 200 (Spring Security protects everything by default) and the unauthenticated
  `POST` answered 403 instead of 401, because the CSRF filter rejected it before authentication. After the
  implementation I ran `./mvnw clean verify` myself: BUILD SUCCESS with 29 tests. `git diff --stat` showed no
  existing file modified (no test, no `pom.xml`, no `application.yaml`), only the new `security` package, and
  the `grep` for Spring, Jakarta and Jackson imports in `domain` and `application` returned nothing.
- **Problems found:** none. The agent's summary matched my own run. One test (`GET /api/local-pokemon`
  answers 401) was green from the start, because Spring denies everything by default. I kept it on purpose
  as a guard test: it fails if a future configuration becomes too permissive.
- **What I changed / fixed:** nothing.
- **Lesson learned:** security rules are easy to specify as tests (which routes are public, which are
  protected, and which status code each case returns). The red run exposed a detail I would have missed
  by only reading the code: without disabling CSRF, an unauthenticated write returns 403, not 401. A test
  that is green before the implementation is not proof of the new behavior; it is a guard, and I label it as
  such.

### Entry 09 - Story 02: Pokémon detail (TDD red/green)

- **Goal:** `GET /api/pokemon/{id}` returning the image, the stats, the description and the evolution chain,
  with 404, 400 and 502 handling, built test-first across the use case, the PokeAPI adapter and the controller.
- **Tool / mode:** GitHub Copilot Chat in IntelliJ, Agent mode. I approved each command one at a time.
- **Prompts used:**

<details>
<summary>Step 4A (tests first, red) and Step 4B (implementation, green)</summary>

```text
Step 4A of "User Story 02: Pokémon detail" (TDD red phase). Follow .github/copilot-instructions.md. Do NOT add or
change any dependency, and do NOT touch pom.xml, application.yaml or any existing test. The tests must fail first,
so every new production method must throw `new UnsupportedOperationException()`; apart from that, skeletons must be
complete enough for Spring to route and bind requests (lesson learned: a skeleton that cannot be bound makes tests fail
for the wrong reason).

Design decisions (already fixed, do not change them):
- Endpoint GET /api/pokemon/{id}. Response fields: id, name, imageUrl, stats [{name, value}], description,
  evolutionChain (list of names).
- The PokeAPI is called in sequence: GET /pokemon/{id}, then GET /pokemon-species/{species.name} (the species name
  comes from the "species.name" field of the pokemon response), then GET /evolution-chain/{chainId}. The chain id is the
  numeric last path segment of "evolution_chain.url" in the species response. The gateway NEVER follows a URL returned
  by the PokeAPI: it always builds the path itself on its configured base URL (https://pokeapi.co/api/v2).
- imageUrl is "sprites.front_default" of the pokemon response. Stats come from "stats[].stat.name" and
  "stats[].base_stat", in the order received. The description is the FIRST "flavor_text_entries" item whose
  "language.name" is "en", with newlines and form feeds ("\n", "\f") replaced by a single space.
- evolutionChain is the chain flattened depth-first (pre-order) into species names, in the order received
  ("chain.species.name", then each "evolves_to" recursively).
- Errors: id < 1 -> InvalidPokemonIdException (400, no PokeAPI call); PokeAPI 404 on /pokemon/{id} ->
  PokemonNotFoundException (404); any other PokeAPI failure, including 404 on the species or the chain, or an
  evolution chain url without a numeric id -> PokemonProviderUnavailableException (502).
- The existing 502 body rule applies: the fixed text, never the internal message.

Create:
1. domain/model: PokemonDetail (record: int id, String name, String imageUrl, List<PokemonStat> stats,
   String description, List<String> evolutionChain) and PokemonStat (record: String name, int value).
2. domain/exception: PokemonNotFoundException (RuntimeException, constructor `(int id)`, message
   "Pokémon with id <id> was not found.") and InvalidPokemonIdException (RuntimeException, constructor `(String message)`).
3. application/port/in/GetPokemonDetailUseCase: `PokemonDetail get(int id);`
4. Add `PokemonDetail findById(int id);` to the existing application/port/out/PokemonGateway, and a skeleton
   implementation in PokeApiPokemonGateway that throws UnsupportedOperationException.
5. application/usecase/PokemonDetailService implements GetPokemonDetailUseCase (constructor receives PokemonGateway;
   no framework annotations; `get` throws UnsupportedOperationException).
6. infrastructure/web: PokemonDetailResponse (record with the response fields) and PokemonStatResponse (record name,
   value), fields only; PokemonDetailController (@RestController, @RequestMapping("/api/pokemon"), constructor receives
   GetPokemonDetailUseCase, `@GetMapping("/{id}") PokemonDetailResponse detail(@PathVariable int id)` throwing
   UnsupportedOperationException). Do NOT modify PokemonController or GlobalExceptionHandler.
7. In infrastructure/config/UseCaseConfig add a GetPokemonDetailUseCase bean (new PokemonDetailService(gateway)) so the
   full application context keeps loading and the existing tests stay green.

Tests (JUnit 5 + AssertJ; follow the style of the existing tests):
A. application/usecase/PokemonDetailServiceTest (Mockito): delegates to gateway.findById and returns its result for
   a valid id (also id 1, the boundary); id 0 and -1 throw InvalidPokemonIdException and never call the gateway
   (parameterized); a PokemonNotFoundException thrown by the gateway is propagated.
B. infrastructure/pokeapi/PokeApiPokemonDetailGatewayTest (WireMock via WireMockExtension, dynamic port, no Spring
   context, RestClient base URL = wireMock.baseUrl() + "/api/v2"; same setup as PokeApiPokemonGatewayTest). EVERY
   stub that returns JSON must send the header "Content-Type: application/json", like the real PokeAPI. Fixtures in
   src/test/resources/pokeapi/: add a "stats" array and a "species": {"name": "bulbasaur"} field to the existing
   pokemon-bulbasaur.json (additive change only, do not break the existing tests), and create
   species-bulbasaur.json (flavor_text_entries with a Spanish entry first, then English entries whose text contains
   "\n" and "\f"; "evolution_chain": {"url": ".../evolution-chain/7/"}, an id different from the pokemon id on purpose,
   to prove the url is not followed) and evolution-chain-7.json (bulbasaur -> ivysaur -> venusaur). Test cases:
   - maps the three responses to a PokemonDetail (image, stats in order, normalized English description,
     evolutionChain [bulbasaur, ivysaur, venusaur]) and calls /pokemon/1, /pokemon-species/bulbasaur and
     /evolution-chain/7 (verify the requests; no trailing slashes);
   - flattens a branching chain depth-first (inline JSON: eevee evolves_to vaporeon and jolteon -> [eevee, vaporeon,
     jolteon]);
   - throws PokemonNotFoundException when /pokemon/{id} answers 404;
   - throws PokemonProviderUnavailableException when /pokemon/{id} answers 500;
   - throws PokemonProviderUnavailableException when the species call answers 404 (and when it answers 500);
   - throws PokemonProviderUnavailableException when the evolution chain call answers 500;
   - throws PokemonProviderUnavailableException when the evolution chain url has no numeric id.
C. infrastructure/web/PokemonDetailControllerTest (@WebMvcTest(PokemonDetailController.class),
   @AutoConfigureMockMvc(addFilters = false), @MockitoBean GetPokemonDetailUseCase; Spring Boot 4 packages as in
   PokemonControllerTest):
   - 200 for GET /api/pokemon/1 with the full JSON body (id, name, imageUrl, stats[0].name/value, description,
     evolutionChain);
   - 404 with content type application/problem+json, status 404 and detail "Pokémon with id 999 was not found."
     when the use case throws PokemonNotFoundException(999);
   - 400 with content type application/problem+json and detail equal to the exception message when the use case
     throws InvalidPokemonIdException("Pokémon id must be greater than or equal to 1.");
   - 502 with application/problem+json and the fixed detail "The Pokémon data provider is currently unavailable.
     Please try again later." when the use case throws PokemonProviderUnavailableException("internal failure"); the
     internal message must not appear in the body.
Use clear test names that describe the behavior.
```

```text
Step 4B of "User Story 02" (TDD green phase). Make ALL the new Story 02 tests pass (PokemonDetailServiceTest,
PokeApiPokemonDetailGatewayTest, PokemonDetailControllerTest) and keep the 29 existing tests green. Follow
.github/copilot-instructions.md. Do not add dependencies. Do NOT modify any test, any test fixture, pom.xml or
application.yaml. Only touch: PokemonDetailService, PokeApiPokemonGateway (findById), PokeApiResponses (additive),
PokemonDetailResponse, PokemonStatResponse, PokemonDetailController and GlobalExceptionHandler (add handlers only).

Design decisions (already fixed, do not change them):
- PokemonDetailService.get(id): if id < 1 throw InvalidPokemonIdException("Pokémon id must be greater than or equal
  to 1.") WITHOUT calling the gateway; otherwise delegate to gateway.findById(id). No framework annotations.
- PokeApiPokemonGateway.findById(id), three sequential calls using relative paths on the configured base URL:
  GET /pokemon/{id}, then GET /pokemon-species/{species.name}, then GET /evolution-chain/{chainId}. The gateway
  never follows a URL returned by the PokeAPI. Use URI variables, never string concatenation.
- The chain id is the last non-empty path segment of "evolution_chain.url"; it must be numeric, otherwise throw
  PokemonProviderUnavailableException. Use it only to build /evolution-chain/{chainId}.
- Errors: a 404 (HttpClientErrorException.NotFound) on /pokemon/{id} -> PokemonNotFoundException(id). Any other
  RestClientException on any of the three calls, including a 404 on the species or the chain ->
  PokemonProviderUnavailableException with the original cause.
- Mapping: imageUrl = sprites.front_default; stats = stats[].stat.name and stats[].base_stat in the order received;
  description = the FIRST flavor_text_entries item with language.name "en", with every run of whitespace
  (including \n and \f) replaced by a single space and trimmed; if there is no English entry (or the list is empty)
  the description is an empty string, never an exception; evolutionChain = chain.species.name followed by each
  evolves_to node, flattened depth-first (pre-order) in the order received; treat a null evolves_to as empty.
- Extend the existing package-private records in PokeApiResponses additively (stats, species, flavor text, evolution
  chain) with @JsonProperty for snake_case and @JsonIgnoreProperties(ignoreUnknown = true); they must not leak out of
  infrastructure.pokeapi. The 8 existing gateway tests must stay green.
- Web: PokemonDetailResponse.from(PokemonDetail) and PokemonStatResponse.from(PokemonStat); the controller only
  delegates to GetPokemonDetailUseCase. In GlobalExceptionHandler add: PokemonNotFoundException -> 404
  ProblemDetail with detail = the exception message; InvalidPokemonIdException -> 400 ProblemDetail with detail = the
  exception message. Do not change the existing handlers.
- Do not use Lombok.
Do the minimum needed. Run ./mvnw clean verify and report the result. If a test fails, tell me why instead of
changing the test.
```

</details>

- **What I checked:**
  - Red run: 45 tests, the 29 existing ones green and the 16 new ones red (5 use case, 7 adapter, 4 controller),
    for the right reasons: `UnsupportedOperationException` from the skeletons, and assertion failures where a
    specific exception was expected. Before committing the red step I read the WireMock stubs: every JSON stub goes
    through one helper that sends `Content-Type: application/json` (the lesson from Entry 05), and the verified
    paths use chain id 7, different from the Pokémon id, to prove the adapter does not follow the URL it receives.
  - Green run: only seven production files changed; no test, fixture, `pom.xml` or `application.yaml` was
    touched; `grep` found no Spring, Jakarta or Jackson imports in `domain` and `application`; `getMessage`
    appears only in the 400 and 404 handlers, and the 502 handler keeps returning the fixed text;
    `./mvnw clean verify`: BUILD SUCCESS with 45 tests.
- **Real API check:** I ran the application and called the endpoint with `curl`: ids 1 and 133 returned the stats,
  a single-line description and the evolution chain, id 999999 returned 404 and id 0 returned 400.
- **Problem found / fix:** a gap in my own prompt, caught before the green step while reviewing the test file
  with a chat assistant: one test uses a species with an empty `flavor_text_entries` list to check an invalid
  chain URL, and my first 4B prompt did not say what to do without an English description. An
  `orElseThrow()` would have failed that test with the wrong exception, so I added the rule (empty string, never
  an exception) to the prompt.
- **Lesson:** fixing the design decisions in the prompt (how paths are built, which errors map to which status,
  what the response contains) left the agent nothing to improvise. The one gap was an edge case I had not
  specified, so from now on I also list the behavior for empty or missing data in every prompt.

## Entry 10: Story 03, local database sync (POST /api/pokemon/{id}/sync, GET /api/local-pokemon)

- **Goal:** persist Pokémon locally with an idempotent sync, keeping proprietary fields (`localizedName`, `region`, `tags`) untouched when a record is synced again.
- **Tools:** Claude (planning, prompt drafting, review of diffs and test output); GitHub Copilot Agent (code generation).
- **Approach:** six TDD steps, each with a failing-test commit followed by an implementation commit: domain validation + use case, JPA adapter, web layer. Every prompt listed the design decisions as already fixed (validation in the domain record, JPA entity separate from the domain model, no Spring annotations in domain/application) so the agent could not improvise.

<details>
<summary>Prompts (condensed)</summary>

- **Red, domain + service:** create `LocalPokemon` record, exceptions, three use case ports, repository port, service skeleton throwing `UnsupportedOperationException`, and tests for sync/upsert/validation. Tests must fail.
- **Green, domain + service:** implement validation in the record's compact constructor and the sync logic. Do not modify tests.
- **Red / Green, persistence:** JPA entity with `@ElementCollection` tags, Spring Data interface, adapter mapping through the domain constructor; `@DataJpaTest` on H2.
- **Red / Green, web:** controller, response DTO, 404 handler, bean wiring in `UseCaseConfig`; do not touch `SecurityConfig`.

</details>

- **What I checked (not the agent's summary):** `git diff --stat` on `src/test` and `pom.xml` was empty after every green step; no `jakarta.persistence` in domain/application; test count grew 45 → 61 → 66 → 72; I read the domain record's constructor and the `UseCaseConfig` and `GlobalExceptionHandler` diffs; manual `curl` against Postgres: sync twice returns the same record and the list has exactly one item, plus 401/400/404 cases.
- **Problem found / fix:** in the web red step the 404 test failed because the skeleton threw before reaching the mocked use case, not because the handler was missing. It is acceptable only because the green prompt required the handler. A gap in my own prompt was also caught in review: nothing in this story throws `InvalidLocalPokemonException` to the controller, so its 400 handler is deferred to Story 04, test first.
- **Lesson:** a validation rule that must hold for every instance belongs in the domain constructor, not in the use case, so the Story 04 update endpoint inherits it without copying code.

## Entry 11: Story 04, update and delete local Pokémon (PUT / DELETE /api/local-pokemon/{id})

- **Goal:** let the user edit only the proprietary fields (`localizedName`, `region`, `tags`) and delete a local record, reusing the domain validation from Story 03.
- **Tools:** Claude (planning, prompt drafting, review of diffs and test output); GitHub Copilot Agent (code generation).
- **Approach:** two TDD cycles, each with a failing-test commit followed by an implementation commit: (1) use cases, service and repository `deleteById`; (2) controller, request DTO and the 400 handler. `PUT` is a full replacement of the three editable fields (idempotent); the service rebuilds a new `LocalPokemon`, so the domain constructor validates and no rule is duplicated. `name`, `imageUrl` and `description` can only change through `sync`.

<details>
<summary>Prompts (condensed)</summary>

- **Red, use cases + persistence:** two new inbound ports, `deleteById` on the repository port, skeleton methods throwing `UnsupportedOperationException`, tests for update/delete in the service (Mockito) and the adapter (`@DataJpaTest`).
- **Green, use cases + persistence:** implement update (find or 404, rebuild the record, save) and delete (find or 404, delete). Do not modify tests or touch config/web.
- **Red, web:** `PUT`/`DELETE` skeletons with real mappings, `UpdateLocalPokemonRequest` record, no handler yet so the 400 test must fail; only two `@MockitoBean` lines may be added to the existing controller test.
- **Green, web:** delegate to the use cases, add only the `InvalidLocalPokemonException` → 400 handler.

</details>

- **What I checked (not the agent's summary):** `git diff --stat` on `src/test`, `pom.xml`, security and config was empty after each green step; no Spring or JPA annotations in domain/application; the only change to the existing controller test was added lines; test count grew 72 → 81 → 88; manual `curl` against Postgres: `PUT`, then `sync` again, and the proprietary fields were preserved; invalid `PUT` → 400, unknown id → 404, `DELETE` → 204 then 404, unauthenticated → 401.
- **Problem found / fix:** a verification `git diff` on the exception handler came back empty while the agent reported adding one. I did not accept either claim: `git status`, `git show` and `grep` showed the handler was uncommitted on disk and the red commit still held the skeleton, so the history was clean. It was my own check that had been unreliable, not the code.
- **Lesson:** one empty diff is not evidence; cross-check with `git status` and `git show` before concluding. Also, a handler for an exception nothing throws yet should wait for the story that needs it, test first.

## 5. Exercise: Task Management API

> To be completed near the end of the project.

### 5.1 Prompt
TODO

### 5.2 Generated code (representative sample)
TODO

### 5.3 How I validated the suggestions
TODO

### 5.4 What I corrected or improved
TODO

### 5.5 Edge cases, authentication and validation
TODO

## 6. Interview cheat sheet

**30-second summary:** TODO (3 sentences: what I built, how I structured it, how AI helped).

**Likely questions** (answer with a real example from section 4):

- *How do you use AI in your day-to-day work?* TODO
- *Tell me about a time the AI gave you wrong or weak code.* TODO (Situation, Task, Action, Result)
- *How do you make sure AI-generated code respects your architecture?* TODO
- *What would you not delegate to AI?* TODO
- *What would you do differently with more time?* TODO
- *Which AI tools did you use in this project, and why two?* I used GitHub Copilot in IntelliJ for code
  generation, because it works directly on the repository and follows the rules in
  `.github/copilot-instructions.md`. I used a chat assistant as a second reviewer: planning the steps,
  drafting prompts, and reviewing diffs and test failures. The decisions were mine and I validated every
  result by running the tests and reading the diff. Examples: the design gap in Entry 03, the stubs
  without `Content-Type` in Entry 05 and the red-for-the-wrong-reason in Entry 06.