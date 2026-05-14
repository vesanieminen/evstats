# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/vesanieminen` contains the Spring Boot + Vaadin server app. Top-level: `Application.java`, `Utils.java`. Key packages: `views/` (with `statistics/` and `charging/` subpackages), `services/` (data loading and APIs), `components/` (shared UI pieces), `model/`, `i18n/`, `util/`.
- `frontend/` contains client resources used by Vaadin (themes in `frontend/themes/evstats/`, static assets in `frontend/src/`).
- Data files live in `src/main/resources/data/` and `src/main/resources/data/excel/`. CSV/XLSX updates typically feed the statistics views.
- Configuration is under `src/main/resources/`: `application.properties`, `application-dev.properties`, `vaadin-featureflags.properties`, and `messages*.properties` (i18n bundles, `en` + `fi`).
- `.devcontainer/` provides a one-command dev setup including a Postgres service via `docker-compose.yml`.

## Build, Test, and Development Commands
- Run locally (dev mode): `./mvnw` or `./mvnw spring-boot:run` (serves at `http://localhost:8080`).
- Production build: `./mvnw clean package -Pproduction` (creates `target/evstats-1.0-SNAPSHOT.jar`).
- Run the packaged app: `java -jar target/evstats-1.0-SNAPSHOT.jar`.
- Integration tests profile: `./mvnw -Pit verify` (starts/stops the app and runs `*IT` tests).

## Coding Style & Naming Conventions
- Java targets version 21; use standard Java formatting (4-space indent, braces on same line).
- Class names are `PascalCase`, methods/fields are `camelCase`. Keep view classes in `views/` (or a topic subpackage like `views/statistics/`) with descriptive names (e.g., `EVRegistrationsView`, `TeslaRegistrationsBarView`).
- Frontend assets use plain JS/CSS. Keep theme files under `frontend/themes/evstats/` and avoid ad-hoc inline styles unless necessary.

## Testing Guidelines
- Unit tests should go under `src/test/java` and use `*Test` suffix (JUnit 5 via Spring Boot starter).
- Integration/UI tests can use Vaadin TestBench with `*IT` suffix (runs via the `-Pit` profile).
- If no tests are added for a change, mention the reason in the PR description.

## Commit & Pull Request Guidelines
- Commit messages are short and imperative, optionally with a topic prefix (e.g., `Bump Vaadin to 24.9.17 and Spring Boot to 3.5.14`, `Devcontainer: install tmux via apt in post-create`, `README: refresh screenshot`).
- PRs should include: a concise summary, affected views/data files, and screenshots for UI changes. Commit screenshots to the repo and reference them by commit SHA rather than by branch/main URL.
- For data updates, note the source file(s) and the date range covered (e.g., `src/main/resources/data/excel/Toukokuu_2023.xlsx`).

## Configuration & Data Tips
- Keep large data updates localized to `src/main/resources/data/` to avoid mixing with code changes.
- Monthly Finnish EV/Tesla registration stats are added by updating the two CSV datasets (and the `TeslaRegistrationsBarView` year range when a new calendar year first appears); the `update-ev-stats` skill automates this.
- When bumping Vaadin or Spring Boot versions, verify that the app still starts in dev mode.
