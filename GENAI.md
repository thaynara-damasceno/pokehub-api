# GenAI usage - pokehub-api

This document records how I used GenAI tools while building this project: what I asked,
what I got back, how I validated it, and what I changed. It also covers the
task-management exercise requested in the technical interview.

## At a glance

- **What is in here:** section 1 is how I work with AI, section 2 the tools, section 3 my validation checklist, section 4 the prompt log (one entry per step up to Entry 08, one per story from Entry 09 on), section 5 the Task Management API exercise, and section 6 my interview cheat sheet.
- **Tools and roles:** GitHub Copilot (Agent mode in IntelliJ) generated almost all application and test code from narrowly scoped prompts (the few manual fixes are in Entries 05 and 06). Claude (chat assistant) planned the steps, drafted and reviewed the prompts, and reviewed diffs and test output. Claude also wrote the Dockerfile, `.dockerignore`, the compose app service, the Postman collection, the OpenAPI security configuration with its test, and the README, which I pasted, reviewed and verified.
- **How I verified:** after every step I read the diff and checked mechanically instead of trusting the tool's summary: a `grep` for framework imports in `domain` and `application`, `git diff --stat` on tests, `pom.xml` and security, the full test suite, and `curl` against the running application. Stories 01 to 04 and authentication were built test-first, and the failing-test commit comes before the implementation commit in the history.
- **Where the AI or my prompts went wrong, and what I did (details in the entries):**
  - Entry 03: its tests rewrote fixture URLs to point at WireMock, which would have forced the adapter to follow URLs returned by an external service; its summary said everything was fine, and I found the gap with a `grep` on the generated test and a second reviewer, then fixed the design with a narrower prompt.
  - Entry 05: a refactor turned the build red because the JSON test stubs had no `Content-Type`; the old code parsed every body as a string and had been hiding it. I confirmed the real header with `curl -I` and fixed the stubs by hand.
  - Entry 06: the agent said all seven tests were red as expected, but four failed for the wrong reason, because my prompt asked for a skeleton without `@RequestParam`; I found it by reading the failure messages.
  - Entry 12: two of my own red-phase prompts produced false reds: a JWT test with a fixed clock that looked expired, and a demo-user skeleton that broke every context test; I fixed both.
  - Section 5: in the Task API exercise the security chain ended with `anyRequest().permitAll()`, and page size had no limit until I added a test and fixed it.
- **A note on format:** entries 01 to 09 are longer because I wrote them before I switched to a compact format (entries 10 to 14).

## 1. Principles

- AI is a pair programmer; **I own the architecture and the decisions**.
- I define the rules first (Clean Architecture, TDD) and give them to the tool as
  project-level instructions (`.github/copilot-instructions.md`).
- Prompts state the exact contract (class names, fields, rules, what NOT to do).
- I do not commit generated code before checking the diff and running the tests.
- Every relevant interaction is logged below (section 4).

## 2. Tools

| Tool | What I used it for |
|---|---|
| GitHub Copilot (IntelliJ) | Code generation: models, adapters, mappers, test scaffolding |
| Claude (chat assistant) | Planning the steps, drafting and reviewing prompts, reviewing diffs and explaining errors. It also wrote the Dockerfile, .dockerignore, compose app service, Postman collection, OpenAPI security configuration with its test, and README, which I pasted, reviewed and verified. Almost all other application and test code was generated with Copilot; the few manual fixes are in Entries 05 and 06 |

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

### Entry 10: Story 03, local database sync (POST /api/pokemon/{id}/sync, GET /api/local-pokemon)

- **Goal:** persist Pokémon locally with an idempotent sync, keeping proprietary fields (`localizedName`, `region`, `tags`) untouched when a record is synced again.
- **Tools:** Claude (planning, prompt drafting, review of diffs and test output); GitHub Copilot Agent (code generation).
- **Approach:** three TDD cycles, each with a failing-test commit followed by an implementation commit: domain validation + use case, JPA adapter, web layer. Every prompt listed the design decisions as already fixed (validation in the domain record, JPA entity separate from the domain model, no Spring annotations in domain/application) so the agent could not improvise.

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

### Entry 11: Story 04, update and delete local Pokémon (PUT / DELETE /api/local-pokemon/{id})

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

### Entry 12: Authentication (register, login, JWT) and demo user

- **Goal:** replace the placeholder HTTP Basic with real authentication: `POST /api/auth/register`, `POST /api/auth/login` returning a signed JWT, bearer validation on every protected route, and a seeded demo user so a reviewer can log in without registering.
- **Tools:** Claude (planning, prompt drafting, review of diffs and test output); GitHub Copilot Agent (code generation).
- **Approach:** six TDD cycles on one branch, each with a failing-test commit followed by an implementation commit: use cases (`AuthService`, plain Java, ports for the user repository, password hasher and token issuer), JPA user adapter, BCrypt hasher and HS256 token issuer, auth controller, switch of `SecurityConfig` to the OAuth2 resource server, and the demo user seeder. Login returns the same generic 401 for an unknown user and for a wrong password.

<details>
<summary>Prompts (condensed)</summary>

- **Red / Green, use cases:** `User` record, three exceptions, ports, `AuthService` as a skeleton, then implemented; registration validation, uniform credential error.
- **Red / Green, persistence:** `UserEntity` (table `app_users`), Spring Data repository, adapter with private mapping.
- **Red / Green, BCrypt and JWT:** plain classes first, then bean wiring and the `pokehub.jwt.*` properties with an environment-variable override.
- **Red / Green, controller:** `AuthController` skeleton, then delegation and three exception handlers (409, 400, 401).
- **Red / Green, security:** tests that issue a real token and call a protected route, then replace `httpBasic()` with the JWT resource server and a `JwtDecoder` bean.
- **Red / Green, seed:** `CommandLineRunner` that registers the demo user and ignores only "already taken", behind a property and a profile.

</details>

- **What I checked (not the agent's summary):** after each green step I checked `src/test`, `pom.xml`, resources and security for unplanned changes (the planned ones are the JWT and seed properties and the test fix listed below); no Spring or JPA annotations in domain/application; no logging of passwords, secrets or tokens; manual `curl` against Postgres: register 201, duplicate 409, short password 400, wrong password and unknown user return the identical 401 message, a valid token opens a protected route, a missing or tampered token returns 401, and the database stores a BCrypt hash (`$2a$10$`), never the password. The demo user can log in, and restarting the application with the user already present starts cleanly.
- **Problems found / fixes:** (1) a red-phase JWT test used a fixed 2026-01 clock with a default decoder that validates expiry against the real clock, so a correct token looked expired; the test was fixed in its own commit, relaxing only the timestamp validator. (2) My seeder red-phase prompt made the skeleton throw inside a `CommandLineRunner`, which broke every Spring context test; the agent stopped instead of touching forbidden files, and I changed the skeleton to an empty body. (3) The agent added `@Profile("!test")` on its own because the test profile inherits the main YAML; I reviewed it and kept it. (4) The agent also removed unused imports in a test I had asked it to change minimally.
- **Lesson:** a red skeleton must never make existing tests fail for the wrong reason, so check the whole suite, not just the new tests. Also, an empty `git diff` is not a result: I cross-check it with `git status` before concluding anything (a pager can also hide short diffs, so I use `git --no-pager diff`).
- **Known limitations (for the README):** the demo credentials and the default JWT secret are demonstration values; BCrypt limits passwords to 72 bytes while validation counts characters; registration reveals whether a username exists; there is no token refresh or revocation.

### Entry 13: Docker packaging (Dockerfile and compose app service)

- **Goal:** run the whole system with one command, `docker compose up --build`, from an empty checkout.
- **Tools:** Claude (wrote the files); I pasted them. GitHub Copilot was not used for this step, and there was no TDD cycle because it is configuration, not behavior.
- **Approach:** multi-stage `Dockerfile` (JDK 21 builds the jar, a JRE 21 image only runs it, as a non-root user), a `.dockerignore`, and an `app` service in `docker-compose.yaml` that waits for the Postgres healthcheck. The database host is passed with `SPRING_DATASOURCE_URL`, so `application.yaml` did not change.
- **What I checked (not the assistant's summary):** I confirmed the Maven Wrapper folder is tracked in Git; reviewed the compose diff (only the new service, plus a trailing newline at the end of the file); and verified from a clean state with `docker compose down -v` and `docker compose build --no-cache`: the application started, the seeded `demo` user could log in against the empty database, the public listing reached PokeAPI from inside the container, and `id` showed the process runs as user `pokehub`, not root.
- **Problem found / fix:** my first run was fully cached and did not prove a clean build, so I repeated it without cache and with a fresh volume. While writing the README I also found a Caffeine dependency that no code used (the cache was planned and never built), and removed it so the `pom.xml` matches the code. All 127 tests and a no-cache image build still passed afterwards.
- **Lesson:** a cached build is not evidence; test the path a reviewer will take. An unused dependency is a claim the project does not keep.

### Entry 14: Postman collection and bearer token support in Swagger

- **Goal:** make the API easy to try without a frontend: a Postman collection that covers the cases built so far, and a way to call the protected endpoints from Swagger UI.
- **Tools:** Claude (generated the collection with a Python script, and wrote the test and the configuration that I pasted). GitHub Copilot was not used for this step.
- **Approach:** the collection has 37 requests in four folders (auth, public Pokémon, local Pokémon, security and docs). Every request has test scripts, the first folder logs in as the demo user and saves the token in a collection variable, and the local Pokémon folder runs in order (sync, sync again, update, sync keeps my data, validation errors, delete). For Swagger, a failing test came first (it reads `/v3/api-docs` and expects a bearer JWT scheme), then I added a small configuration class that declares the scheme and a `@SecurityRequirement` on the controller that holds sync and the local endpoints. The public endpoints stay without a lock.
- **What I checked (not the assistant's summary):** I ran the collection with Newman against the running application: 37 requests, 53 assertions, 0 failed. The test was red before the change: two of its three cases failed because the scheme did not exist, and the third (public listing has no security) already passed, so it is a guard, not a red. After the change the full suite had 130 passing tests, up from 127. In Swagger UI I saw the Authorize button, logged in, pasted the token, and `POST /api/pokemon/25/sync` returned 200. I also cloned the public repository into an empty folder and the build passed there with the same 130 tests.
- **Problem found / fix:** before this change Swagger UI had no Authorize button, so every protected endpoint returned 401 when tried from the page. I only noticed it by opening the page and looking, not from any test.
- **Lesson:** a test on the API document proves what the document says, not what the screen does; I checked both. A collection that passes against the real application is worth more than one that only looks complete.

## 5. Exercise: Task Management API

This exercise is separate from the PokeHub project. I ran it in a local scratch repository that is not published, so the generated code would not mix with the reviewed project. This section holds the prompt, representative samples, and what I found when I reviewed the output.

### 5.1 Prompt

- **Tool:** GitHub Copilot (Agent mode, IntelliJ) with a project instruction file (`.github/copilot-instructions.md`) adapted from the one in this repository: same architecture and TDD rules, package `com.example.tasks`, no PokeAPI references.
- **How the prompt was made:** I drafted it with Claude's help and used it as written. It states the architecture, the model, the endpoints, the edge cases and what NOT to do, and it asks the tool to stop after the domain and application layers so I can review before any infrastructure exists.

```text
Generate a RESTful Task Management API in Java 21 with Spring Boot, Maven, following Clean Architecture and TDD. Package com.example.tasks.

Architecture (strict):
- domain: plain Java records/enums and rules, no Spring/JPA/Jackson imports.
- application: use cases and ports (interfaces), plain Java, no Spring imports.
- infrastructure: REST controllers, DTOs, JPA entities and adapters, security config. JPA entities must never reach controllers; map them to domain objects inside the adapters.

Model:
- Task: id (UUID), title, description, status, dueDate, ownerUsername.
- status is an enum TODO, IN_PROGRESS, DONE (default TODO).
- User: username (unique), passwordHash. Assume a basic User model exists; implement only register and login.

Endpoints (standard verbs, consistent JSON, RFC 9457 ProblemDetail errors):
- POST /api/auth/register, POST /api/auth/login (returns a signed JWT).
- POST /api/tasks (201), GET /api/tasks (paginated, optional ?status=), GET /api/tasks/{id}, PUT /api/tasks/{id} (full replacement), DELETE /api/tasks/{id} (204).
- Every /api/tasks route requires a valid bearer token.

Rules and edge cases:
- A user can only see and change their own tasks. For another user's task return 404, not 403, so existence is not leaked.
- title required, 1 to 120 characters; description optional, up to 2000; dueDate optional ISO-8601 date and not in the past on create; status must be a valid enum value.
- Malformed JSON, invalid enum or invalid UUID return 400 with a clear message. Unknown id returns 404.
- Passwords hashed with BCrypt; the JWT secret comes from configuration, never hardcoded. Never log passwords or tokens. Login returns the same 401 for an unknown user and a wrong password.
- Use parameterized access only (Spring Data), no string-built queries.

Process:
- Work test-first: for each layer write failing tests (JUnit 5, Mockito for the application layer, @DataJpaTest on H2 for persistence, MockMvc for controllers and security), then the implementation.
- Do NOT put business rules in controllers. Do NOT modify tests to make them pass. Do NOT add features I did not ask for.
- Start with the domain and application layers and stop for my review before generating the infrastructure.
```

<details>
<summary>Follow-up prompts (condensed)</summary>

- **Stage 2:** generate the infrastructure test-first (JPA entities separate from the domain records, adapters that look tasks up by id AND owner, stateless HS256 JWT with the resource server already in the `pom.xml`, BCrypt, controllers and DTOs, one exception handler mapping to ProblemDetail, use cases wired as beans). The owner always comes from the token. Do not add dependencies and do not modify the domain, the application code or existing tests.
- **Fix, red:** add a failing test that `TaskPageRequest` rejects a size above 100 and accepts 100; add only, then stop.
- **Fix, green:** change only `TaskPageRequest`: a named constant for the maximum and an `IllegalArgumentException` above it. Do not touch tests or the `pom.xml`.

</details>

### 5.2 Generated code (representative sample)

Three short samples from the output, all of which I read.

**Domain rules live in the record constructor, and "today" is passed in (`Task.java`):**

```java
public record Task(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        String ownerUsername) {

    public Task {
        if (id == null) {
            throw new ValidationException("Task id is required");
        }
        if (title == null || title.isBlank() || title.length() > 120) {
            throw new ValidationException("Title must contain 1 to 120 characters");
        }
        if (description != null && description.length() > 2000) {
            throw new ValidationException("Description must not exceed 2000 characters");
        }
        if (ownerUsername == null || ownerUsername.isBlank()) {
            throw new ValidationException("Task owner is required");
        }
        status = status == null ? TaskStatus.TODO : status;
    }

    public static Task create(
            UUID id,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            String ownerUsername,
            LocalDate today) {
        if (dueDate != null && dueDate.isBefore(today)) {
            throw new ValidationException("Due date must not be in the past");
        }
        return new Task(id, title, description, status, dueDate, ownerUsername);
    }

    public static Task replace(
            UUID id,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            String ownerUsername) {
        return new Task(id, title, description, status, dueDate, ownerUsername);
    }
}
```

**Another user's task is simply not found (`GetTaskUseCase.java`):**

```java
public Task execute(UUID id, String ownerUsername) {
    return taskRepository.findByIdAndOwner(id, ownerUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Task"));
}
```

**Route protection (`SecurityConfiguration.java`):**

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/tasks", "/api/tasks/**").authenticated()
        .anyRequest().permitAll())
.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
.oauth2ResourceServer(resourceServer -> resourceServer
        .authenticationEntryPoint(authenticationEntryPoint)
        .jwt(Customizer.withDefaults()))
```

### 5.3 How I validated the suggestions

- **In stages.** The tool stopped after the domain and application layers, as the prompt asked, and I reviewed them before asking for the infrastructure.
- **Reading.** I read `Task`, `CreateTaskUseCase`, `GetTaskUseCase`, `LoginUseCase`, `ReplaceTaskUseCase`, `TaskController`, `TaskRequest`, `TaskPageRequest`, the filter chain of the security configuration and the main part of the exception handler.
- **Mechanical checks (same method as section 3).** No Spring, Jakarta or Jackson imports in `domain` or `application` (`grep` returned nothing); no dependency added (`git diff` on `pom.xml` was empty); the JWT secret is read from configuration, with no default value and a 32-byte minimum check; no logging of passwords or tokens (`grep` returned nothing); the infrastructure step changed only one existing file, application.properties, by one line (the JWT secret property).
- **Tests.** The suite passed after each stage: 21 tests after the domain and application layers, 30 after the infrastructure, 31 after my fix.
- **Running it.** I started the application and attacked it with `curl` (table in 5.5).
- **What I did not do.** I did not read the JPA entities and adapters, the response DTOs or the integration tests line by line, and I did not exercise cross-user access by hand: for that, I rely on the agent's report that its integration tests cover it.

### 5.4 What I corrected or improved

**Fixed: unbounded page size.** `curl` with `?size=100000` returned 200. The prompt never asked for a limit, so the tool did not add one. I fixed it test-first in two commits: a failing test (size 101 is rejected, size 100 is accepted) and then the minimum change.

```diff
 public record TaskPageRequest(int page, int size) {
 
+    public static final int MAX_PAGE_SIZE = 100;
+
     public TaskPageRequest {
         ...
+        if (size > MAX_PAGE_SIZE) {
+            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
+        }
     }
 }
```

After the fix the same request returns 400, and `size=100` still returns 200.

**Found but not fixed:**

| Finding | How I know |
|---|---|
| A repeated username returns 400 instead of 409: the handler groups `UsernameAlreadyExistsException` with validation errors | Confirmed with `curl`; handler read |
| The security chain ends with `anyRequest().permitAll()`, so a new route outside `/api/tasks` and `/api/auth` would be public by default; the safe default is to require authentication | Read the configuration |
| The handler catches every `IllegalArgumentException`, which could turn an unrelated library error into a 400 and hide a real bug | Read the handler (a risk, not observed) |
| The same title and description rules exist twice, in `TaskRequest` (Bean Validation) and in the `Task` constructor, and they can drift apart | Read both |
| `Task.replace` only calls the constructor, so it adds no rule beyond it | Read the code |
| Login skips the BCrypt check when the user does not exist, so response time could reveal valid usernames | Reasoning from the code, not measured |

The agent also reported that the Spring Boot 4 test annotation packages differ from the ones it used first, and it corrected its own imports.

**Lesson:** what the prompt does not say, the tool decides alone. The page size limit and the 409 were gaps in my prompt as much as in the output.

### 5.5 Edge cases, authentication and validations

**Authentication.** Stateless JWT (HS256) with BCrypt for passwords. Every `/api/tasks` route requires a token and `/api/auth/**` is public. The task owner is the token subject (`jwt.getSubject()`) and never comes from the request body. Login throws the same exception for an unknown user and a wrong password.

**Validation.** Field rules live in the domain constructor (and are repeated in the request DTO, see 5.4). The "due date not in the past" rule applies on creation only, as the prompt asked, so editing an old task does not fail because its date has passed.

**Ownership.** Lookups are by id and owner, so another user's task is "not found" and its existence is not leaked.

**Edge cases I ran against the running application:**

| Request | Status |
|---|---|
| Register a new user | 201 |
| Register the same username again | 400 (409 would be better) |
| `?page=-1` | 400 |
| `?size=100000` before the fix | 200 |
| `?size=100000` after the fix | 400 |
| `?size=100` after the fix | 200 |
| `?status=LIXO` (invalid enum) | 400 |
| `/api/tasks/not-a-uuid` | 400 |
| Malformed JSON body | 400 |
| No token | 401 |

## 6. Interview cheat sheet

**30-second summary:** I built a Spring Boot REST API on top of PokeAPI with Clean Architecture: paginated listing, detailed view, local sync with proprietary fields, edit and delete, JWT authentication and Docker, covered by 130 automated tests. The domain and application layers are plain Java behind ports, and every story was built test-first, with the red-to-green commits visible in the history. I used Copilot to generate code from tightly scoped prompts and Claude to plan and review, and I verified each step myself with diffs, mechanical checks and the running application.

**Likely questions** (answer with a real example from section 4):

- *How do you use AI in your day-to-day work?* As a pair programmer, not an author. I fix the design first, give the tool a narrow contract (names, rules, what not to touch), let it write the failing tests and then the minimum code, and I review every diff and run the tests before moving on. I also use a chat assistant to plan the steps and to review output and error messages.

- *Tell me about a time the AI gave you wrong or weak code.*
  - **Situation:** while building the PokeAPI gateway, my prompt did not say how the adapter builds the detail URL, and Copilot filled the gap itself when it generated the WireMock tests.
  - **Task:** I needed tests that described the behavior I wanted, not the tool's assumptions.
  - **Action:** the agent's summary said everything was fine, so I did not rely on it: a `grep` on the generated test showed it rewrote the fixture `url` fields to point at WireMock, which would have forced the adapter to follow URLs returned by an external service. I confirmed it with a chat assistant as a second reviewer, decided the design myself, and sent a second prompt limited to the test code: the adapter builds every path from the configured base URL.
  - **Result:** the tests and the adapter were corrected, and the gateway never follows returned URLs. From then on, every prompt listed the design decisions as already fixed, so the tool could not improvise them.

- *How do you make sure AI-generated code respects your architecture?* Three things. The rules live in a project instruction file the tool reads. Each prompt states the fixed design decisions and an explicit "do not touch" list. And I verify mechanically instead of trusting the tool's summary: a `grep` for framework imports in `domain` and `application`, `git diff --stat` on tests, `pom.xml` and security after every green step, and the full test suite. The red/green commit history makes all of it auditable.

- *What would you not delegate to AI?* Architecture and trade-offs, security-sensitive choices such as secrets and authentication defaults, deciding what "done" means, and understanding the code well enough to explain it. In the Task API exercise the tool left `anyRequest().permitAll()` at the end of the security chain, which is exactly the kind of default I would not hand over. The AI drafts; I own the decisions and the verification.

- *What would you do differently with more time?* Add the optional caching layer for PokeAPI responses (I removed an unused caching dependency rather than leave a claim the code did not keep), replace `ddl-auto: update` with Flyway migrations, paginate the local collection, run the persistence tests against a real PostgreSQL with Testcontainers instead of H2, and handle concurrent first-time syncs of the same Pokémon.

- *Which AI tools did you use in this project, and why two?* I used GitHub Copilot in IntelliJ for code
  generation, because it works directly on the repository and follows the rules in
  `.github/copilot-instructions.md`. I used a chat assistant as a second reviewer: planning the steps,
  drafting prompts, and reviewing diffs and test failures. The decisions were mine and I validated every
  result by running the tests and reading the diff. Examples: the design gap in Entry 03, the stubs
  without `Content-Type` in Entry 05 and the red-for-the-wrong-reason in Entry 06.