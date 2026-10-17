# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3.3.3 REST API (Java 21) behind the `erp-ui-react` `tank-board` app: lists a user's tank boards (the UI calls them facility maps) and the shared templates they may clone, and manages clones (clone, rename, save settings, delete). Created by spec 018. Built on `erp-core-java`, like `erp-api-entity-mgmnt-java`, but its tank board entity, repository and VOs live in this repo, not in core (see `erp-start-here/decisions/0003`).

## Prerequisites

`erp-core-java` must be in the local Maven repo. Its coordinates are `com.wc_matthew.demo:erp-core-java:0.1.0` (what its pom actually declares; `erp-api-entity-mgmnt-java` references a `com.wc_matthew.demo.erp.core` groupId that core doesn't publish). Use a **clean** install: `erp-core-java/target/classes` has been seen holding IDE-compiled classes without Lombok, which a plain `mvn install` packages as-is and which then break every Lombok getter here ("cannot access Getter").

```bash
cd ../erp-core-java && mvn clean install
```

Java 21 because that is the installed JDK (entity-mgmnt targets 22).

## Commands

```bash
mvn test                # all tests, no database needed
mvn spring-boot:run     # port 5184; needs .env (copy .env.example) and MariaDB from erp-docker-deps
```

## Known blocker: app startup

The full app does **not** start yet, and the cause is in `erp-core-java`, not here. `VarietalCompEntry.varietal`, `IngredientCompEntry.ingredient` and `HarvestMethodCompEntry.harvestMethod` are entity-typed fields with no `@ManyToOne`, so Hibernate fails to build the persistence unit ("Could not determine recommended JdbcType for Java type ...Varietal"). This affects every service that scans core's entities. `TankBoardRepoTest` avoids it by scanning only `Facility` and `TankBoard`.

## Configuration

`application.properties` reads `DB_*`, `DDL_AUTO` (default `none`; `update` on a first run creates the tables) and `SEED_DEMO_TEMPLATES` (default `true`) from `.env` via springboot3-dotenv. Unlike entity-mgmnt there is no hand-built `DataSource` bean; Spring Boot's auto-configured one is used.

`TankBoardTemplateSeeder` gives every `Facility` with no shared boards the four demo templates from spec 003 (bulk intake, packaging, west/east tank farm, with the same role lists as the UI mock). It logs and skips if the tables don't exist.

## Architecture

Package `com.wc_matthew.demo.erp.tank_board`; `ErpApiTankBoardApplication` component-scans all of `com.wc_matthew.demo.erp` (to pick up core's config, services and `GlobalExceptionHandler`) and adds this repo's entity and repo packages.

```
controller/   TankBoardController (/tank-board), BaseController (same helpers as entity-mgmnt)
service/      TankBoardService → impl/TankBoardServiceImpl
repo/         TankBoardRepo (JPQL queries)
entity/       TankBoard
vo/           request/response value objects
middleware/   TankBoardExceptionHandler
config/       TankBoardTemplateSeeder
```

**Endpoints.** Every call identifies the caller with the headers `erp-username`, `erp-user-role` and `erp-facility-id` (`TankBoardHeaders`), a stand-in until core's `SecurityService` validates tokens. A missing or malformed header is a 400.

| Method | Path | Result |
|---|---|---|
| GET | `/tank-board/templates` | shared boards at the user's facility that list their role, by title |
| GET | `/tank-board/boards` | the user's own active clones, oldest first |
| GET | `/tank-board/boards/{id}` | `TankBoardDetailVO` (adds `settings`, timestamps) for a visible board |
| POST | `/tank-board/boards` | clone: body `{sourceId, title, description?}` → 201 + `Location`, `TankBoardSummaryVO` |
| PUT | `/tank-board/boards/{id}` | rename a clone: `{title, description?}` |
| PUT | `/tank-board/boards/{id}/settings` | replace a clone's settings with the body (any JSON object) |
| DELETE | `/tank-board/boards/{id}` | archive a clone → 204 |

The UI's list screen is `/templates` followed by `/boards`. `TankBoardSummaryVO` has the same fields as the UI's `FacilityMapSummary`, with numeric ids, and `kind` serializes as `"shared"` / `"clone"`; `CloneTankBoardRequestVO` matches `CloneFacilityMapInput`.

**Rules (in `TankBoardServiceImpl`).** A shared board is visible when it is active, at the user's facility and lists their role; a clone only to its owner (any facility, matching the UI mock). A board the user can't see is a 404 (`EntityNotFoundException`), not a 403. Only shared boards can be cloned, and the clone copies the source's settings. Only clones can be renamed, have settings saved, or be deleted; trying any of these on a shared board is a 400. Titles are trimmed, required and at most 100 characters; a blank description is stored as null. Delete is soft (`isActive = false`), like entity-mgmnt. Roles are compared lower-cased (`TankBoardUserVO.of` normalizes them), as the UI mock does.

**`TankBoard` entity** (`tank_board`, extends core's `BaseDictionaryEntity`): `name` (the VO's `title`), `kind`, `facility` (`@ManyToOne` core `Facility`), `ownerUsername` and `source` (clones only), `roles` (element collection `tank_board_role`, shared only), `settings` (`Map` in a JSON column, free-form because the customizations aren't defined yet). A `@PrePersist`/`@PreUpdate` check makes a shared board with an owner or source, or a clone missing either, fail on every write path (the decision 0001 pattern).

**Errors.** `TankBoardExceptionHandler` runs ahead of core's `GlobalExceptionHandler`, which builds the `app-err-code` / `default-message` / `message` headers but never attaches them, and fails on exceptions without an HTTP status (no core exception sets one). This handler attaches the headers and maps the error code to a status (ERP-1003 → 404, ERP-1005/1006/1008 → 400, ERP-1004 → 409, else 500). Spring's missing header, type mismatch and unreadable body errors become ERP-1008 400s.

## Tests

JUnit 4 + Mockito, the same style as entity-mgmnt: `TankBoardServiceImplTest` (rules, mocked repo), `TankBoardControllerTest` (standalone MockMvc with the exception handler: routing, headers, JSON shape, statuses), `TankBoardTest` (entity integrity check), and `TankBoardRepoTest` (`@DataJpaTest` on in-memory H2 in MySQL mode: the JPQL queries, role collection, JSON settings and the seeder).
