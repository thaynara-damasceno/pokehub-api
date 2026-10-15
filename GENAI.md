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
| TODO: other tools | TODO: describe honestly (e.g. chat assistant for planning and learning) |

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

> **Step / goal:**
> **Prompt used:** (exact text)
> **What the AI generated:**
> **What I checked:**
> **Problems found:**
> **What I changed / fixed:**
> **Lesson learned:**

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
  have forced the adapter to follow URLs returned by an external service. I spotted it in
  the review (a `replace(...)` of `https://pokeapi.co/api/v2` in the test) and not from the agent's summary,
  which said everything was fine.
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