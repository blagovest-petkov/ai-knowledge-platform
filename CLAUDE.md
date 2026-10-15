# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

AI-powered knowledge platform (Java 21, Spring Boot 4.1, Maven, PostgreSQL, Flyway, Lombok). It is currently in **Stage 1**: a modular monolith that accepts PDF uploads, stores the original file on the local filesystem, extracts text, splits it into chunks, and persists metadata and chunks in PostgreSQL. Later stages (Docker, AWS ECS/RDS/S3, SQS workers, embeddings + pgvector + LLM, Terraform, GitHub Actions) are planned but deliberately **not** to be implemented early. See `docs/architecture.md` for the roadmap.

The `docs/` folder is the spec:
- `docs/stage-1-tasks.md`: ordered task checklist; tasks get ticked `[x]` as they're completed (commits often just update this file).
- `docs/stage-1-definition-of-done.md`: acceptance criteria for Stage 1.
- `docs/api.md`: REST contract (`POST/GET /documents`, `GET/DELETE /documents/{id}`), including response shapes and error format.
- `docs/database.md`: schema for `documents` and `document_chunks`.

Implementation should match these docs. If the code has to diverge from them, update the docs in the same change.

## Commands

```bash
./mvnw spring-boot:run                     # run the app (needs local PostgreSQL)
./mvnw clean package                       # build + run tests
./mvnw test                                # all tests
./mvnw test -Dtest=ClassName               # single test class
./mvnw test -Dtest=ClassName#methodName    # single test method
```

There is no linter or formatter configured.

## Local environment

- PostgreSQL at `localhost:5432`, database `ai_knowledge_db`, configured in `src/main/resources/application.properties`. The existing `@SpringBootTest` context test also needs this database to be running.
- The schema is owned entirely by Flyway (`src/main/resources/db/migration`, `V<n>__description.sql`), with `spring.jpa.hibernate.ddl-auto=validate`. Schema changes always go in a new migration; never edit an applied one. JPA entities have to match the migrated schema exactly or startup fails.
- Uploaded files are written to `file.upload-dir` (`./uploads`).
- Config is `application.properties`, even though some docs say `application.yml`.

## Architecture rules

- **Organize by business module, not by technical layer.** Packages go under `com.example.ai_knowledge_platform`: `document`, `search`, `question`, `common`. Each module has its own `controller/`, `service/`, `repository/`, `model/` as needed. Don't create global `controller/service/repository` packages. `search` and `question` are placeholders in Stage 1.
- Dependencies flow Controller → Service → Repository/Infrastructure. Controllers handle HTTP mapping and validation only. Services must not depend on HTTP types (e.g. `HttpServletRequest`).
- Infrastructure sits behind interfaces so it can be swapped in later stages:
  - `DocumentStorage` (`store`/`retrieve`/`delete` by UUID) → `LocalDocumentStorage` now, `S3DocumentStorage` later.
  - `DocumentTextExtractor` → `PdfDocumentTextExtractor`.
  - `TextChunker` (`List<String> chunk(String text)`), kept independent of extraction.
- Original PDFs are never stored in PostgreSQL. The DB keeps only `storage_path`.
- Document status lifecycle: `UPLOADED → PROCESSING → PROCESSED | FAILED`. It's an enum in Java and backed by the `statuses` lookup table, which `documents.status` references via FK (V1 migration). Processing is synchronous: success returns `201` with `PROCESSED`, failure returns `500` with the document marked `FAILED`.
- File writes and DB transactions are not atomic together, so failures after the file is stored have to be handled explicitly to avoid inconsistent state.
- `document_chunks`: UUID PK, FK to `documents.id` with `ON DELETE CASCADE`, unique `(document_id, chunk_index)`, zero-based `chunk_index`.
- Errors are handled centrally with `@RestControllerAdvice`, using a consistent body (`status`, `error`, `message`, `timestamp`) and never exposing stack traces.
- Only `application/pdf` uploads are accepted. Other types return `400`.
- Guiding principle: start simple and add abstractions or infrastructure only when there's a current reason.

## Testing

The Stage 1 stack is JUnit 5, Mockito, and Spring Boot Test. Business logic (DocumentService, TextChunker) should be unit-testable without a web server or DB. Testcontainers is planned for a later stage, so don't add it yet.
