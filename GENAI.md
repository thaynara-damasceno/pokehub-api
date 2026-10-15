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

- **Goal:** create `Pokemon`, `PokemonPage`, ports and `ListPokemonService` skeleton, plus
  the tests first (TDD red).
- **Prompt used:** TODO paste the exact prompt.
- **What the AI generated:** TODO
- **What I checked:** no framework imports in domain/application, service without
  annotations, tests fail for the right reason (`UnsupportedOperationException`),
  gateway never called on invalid input.
- **Problems found:** TODO (write "none" if none)
- **What I changed / fixed:** TODO
- **Lesson learned:** TODO

### Entry 02 - Story 01, Part 1B: pagination rules (TDD green)

- **Goal:** implement the minimum in `ListPokemonService` to make the tests pass.
- **Prompt used:** TODO
- **What the AI generated:** TODO
- **What I checked:** tests unchanged, only validation + delegation added.
- **Problems found:** TODO
- **What I changed / fixed:** TODO
- **Lesson learned:** TODO

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