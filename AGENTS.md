# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/vesanieminen` contains the Spring Boot + Vaadin server app. Key areas: `views/` (UI views), `services/` (data loading and APIs), `components/` (shared UI pieces), and `util/`.
- `frontend/` contains client resources used by Vaadin (themes in `frontend/themes/evstats/`, static assets in `frontend/src/`).
- Data files live in `src/main/resources/data/` and `src/main/resources/data/excel/`. CSV/XLSX updates typically feed the statistics views.
- Configuration is under `src/main/resources/` (`application.properties`, `application-dev.properties`, feature flags).

## Build, Test, and Development Commands
- Run locally (dev mode): `./mvnw` or `./mvnw spring-boot:run` (serves at `http://localhost:8080`).
- Production build: `./mvnw clean package -Pproduction` (creates `target/evstats-1.0-SNAPSHOT.jar`).
- Run the packaged app: `java -jar target/evstats-1.0-SNAPSHOT.jar`.
- Integration tests profile (if/when added): `./mvnw -Pit verify` (starts/stops the app and runs `*IT` tests).

## Coding Style & Naming Conventions
- Java targets version 21; use standard Java formatting (4-space indent, braces on same line).
- Class names are `PascalCase`, methods/fields are `camelCase`. Keep view classes in `views/` with descriptive names (e.g., `EVRegistrationsView`).
- Frontend assets use plain JS/CSS. Keep theme files under `frontend/themes/evstats/` and avoid ad-hoc inline styles unless necessary.

## Testing Guidelines
- Unit tests should go under `src/test/java` and use `*Test` suffix (JUnit 5 via Spring Boot starter).
- Integration/UI tests can use Vaadin TestBench with `*IT` suffix (runs via the `-Pit` profile).
- If no tests are added for a change, mention the reason in the PR description.

## Commit & Pull Request Guidelines
- Commit messages are short, sentence-style, and typically start with “Update/Updated …” (e.g., “Update Vaadin version.”).
- PRs should include: a concise summary, affected views/data files, and screenshots for UI changes.
- For data updates, note the source file(s) and the date range covered (e.g., `src/main/resources/data/excel/Toukokuu_2023.xlsx`).

## Configuration & Data Tips
- Keep large data updates localized to `src/main/resources/data/` to avoid mixing with code changes.
- When bumping Vaadin or Spring Boot versions, verify that the app still starts in dev mode.
