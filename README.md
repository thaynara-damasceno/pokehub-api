# pokehub-api

**PokeHub: Pokémon Data Management API.** A RESTful backend built with Java 21 and Spring Boot 4 that
lists and details Pokémon from the public [PokeAPI](https://pokeapi.co), lets an authenticated user
sync Pokémon into a local PostgreSQL database, and enrich them with proprietary data (localized name,
region, tags). Backend only (no frontend), as agreed with the recruiter.

## Quick start

Requirements: Docker with Compose. Nothing else.

```bash
docker compose up --build
```

The API is then available at `http://localhost:8080` and the interactive docs at
`http://localhost:8080/swagger-ui/index.html`. A demo user is created on the first start.

| What | Value |
|---|---|
| Demo user | `demo` / `demo-password-123` |
| PostgreSQL | database `pokemon`, user `pokemon`, password `pokemon` (port 5432) |
| JWT secret | default demo value in `application.yaml`; override with the `JWT_SECRET` environment variable |
| Token lifetime | 3600 seconds; override with `JWT_TTL_SECONDS` |

All of these are demonstration values committed on purpose so the project runs with one command.
They must be replaced in any real deployment.

Run the tests (H2 in memory, no Docker needed): `./mvnw test`

Develop locally with only the database in Docker: `docker compose up -d postgres`, then `./mvnw spring-boot:run`.

## Try it

```bash
# list Pokémon (public)
curl "localhost:8080/api/pokemon?page=0&size=5"

# log in and keep the token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo-password-123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# sync Pikachu into the local database, then add proprietary data
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/api/pokemon/25/sync
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"localizedName":"Pikachu","region":"Kanto","tags":["electric","starter"]}' \
  localhost:8080/api/local-pokemon/25

# syncing again refreshes the PokeAPI data and keeps your proprietary data
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/api/pokemon/25/sync
```

A Postman collection with every case (success, validation errors, 401s, sync idempotency) is in [docs/postman](docs/postman/pokehub-api.postman_collection.json). Import it and run the collection in order, or from a terminal: `npx newman run docs/postman/pokehub-api.postman_collection.json`.

## Endpoints

| Method and path | Access | Description |
|---|---|---|
| `GET /api/pokemon?page=&size=` | public | Paginated list from PokeAPI (page from 0, default size 20, max 50) |
| `GET /api/pokemon/{id}` | public | Detail: image, stats, English description, evolution chain |
| `POST /api/auth/register` | public | Create a user (201; 409 if taken; 400 if invalid) |
| `POST /api/auth/login` | public | Returns a JWT (401 with a generic message on any bad credential) |
| `POST /api/pokemon/{id}/sync` | token | Idempotent upsert into the local database |
| `GET /api/local-pokemon` | token | All locally stored Pokémon |
| `GET /api/local-pokemon/{id}` | token | One local Pokémon (404 if missing) |
| `PUT /api/local-pokemon/{id}` | token | Replace `localizedName`, `region` and `tags` (404 / 400) |
| `DELETE /api/local-pokemon/{id}` | token | Remove a local Pokémon (204; 404) |

Application errors use RFC 9457 `ProblemDetail`; a missing or invalid token gets a 401 from Spring Security with an empty body. A PokeAPI failure returns 502 with a fixed generic message; the
real cause is only written to the server log.

## Architecture

```text
infrastructure  (Spring, HTTP, JPA, PokeAPI client, security)
      |  depends on
application     (use cases and ports, plain Java)
      |  depends on
domain          (records and rules, plain Java)
```

- **Dependencies point inward.** `domain` and `application` have no Spring, JPA or Jackson imports, so
  the business rules are tested with plain JUnit and Mockito, in milliseconds.
- **Ports and adapters.** Use cases depend on interfaces (`PokemonGateway`, `LocalPokemonRepository`,
  `UserRepository`, `PasswordHasher`, `TokenIssuer`); the infrastructure implements them.
- **Wiring is explicit.** Services are plain classes registered as beans in `@Configuration` classes.
- **JPA entities are separate from the domain model.** The adapters map between them, and entity to
  domain mapping goes through the domain constructor, so validation also runs on read.
- **Validation lives where the invariant lives.** Rules about a Pokémon's own data (name and region up to
  60 characters, up to 10 tags of up to 30 characters, no duplicates) are in the `LocalPokemon`
  constructor, so create and update paths cannot bypass them. Rules about how an operation is called
  (page and size, id at least 1) are in the use case.

### Decisions worth knowing

- **PokeAPI client.** Detail pages are fetched concurrently with virtual threads and the order of the
  page is preserved. The client builds every path itself and never follows URLs returned by the
  provider.
- **Sync is an upsert.** The PokeAPI fields (name, image, description) are refreshed; `localizedName`,
  `region` and `tags` are never overwritten by a sync. Syncing the same Pokémon repeatedly never creates
  a duplicate (the local id is the PokeAPI id, which is also the primary key).
- **Authentication.** Stateless JWT (HS256) validated by Spring Security's resource server. Passwords are
  hashed with BCrypt. Unknown user and wrong password produce the same 401 message.
- **Reading is public, writing is protected.** Listing and detail are public data; everything that
  writes, and the whole local collection, requires a token.

## How it was built

Every story was built test-first, and the red to green sequence is visible in the commit history
(`test:` commit, then `feat:` commit, with `refactor:` where needed). The project was built with
GenAI tools (GitHub Copilot for code generation; Claude for planning and review, and it also wrote the Dockerfile and this README). The full account,
including the prompts, what I checked, and the problems found, is in [GENAI.md](GENAI.md).

## Known limitations

- Database schema is managed by `ddl-auto: update`; a real deployment should use migrations (Flyway or Liquibase).
- No caching: each request calls PokeAPI. The detail endpoint makes three sequential calls.
- `GET /api/local-pokemon` is not paginated.
- The evolution chain is flattened depth-first, so branching chains lose their tree shape.
- Weight is returned as PokeAPI provides it (hectograms), without conversion.
- Tag duplicates are case-sensitive (`Fire` and `fire` are different tags).
- BCrypt has a 72-byte limit, while the password length check counts characters, so a password with accented characters can pass the check and still exceed 72 bytes.
- Registration reveals whether a username exists; there is no rate limiting, token refresh or revocation.
- Concurrent first-time syncs of the same Pokémon are not handled specially.
- Demo credentials and the default JWT secret are committed for convenience.
- The only demo data is the `demo` user; the local Pokémon collection starts empty until a sync.