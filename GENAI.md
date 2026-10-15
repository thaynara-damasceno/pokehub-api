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

(Add one entry per relevant interaction.)

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