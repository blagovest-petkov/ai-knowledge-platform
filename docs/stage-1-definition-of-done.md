````md id="58321"
# Stage 1 — Definition of Done

## Goal

Stage 1 establishes the local foundation of the AI Knowledge Platform.

The application must be able to accept a PDF document through a REST API, store the original file locally, extract its text, split the text into chunks, and persist the document metadata and chunks in PostgreSQL.

No AI, embeddings, vector search, AWS, Docker, or asynchronous processing is required for Stage 1.

---

# 1. Project Setup

- [ ] Java 21 is installed and configured.
- [ ] IntelliJ IDEA project is created.
- [ ] Spring Boot application starts successfully.
- [ ] Maven build works successfully.
- [ ] Git repository is connected to GitHub.
- [ ] `.gitignore` is configured.
- [ ] Project can be cloned and built from a clean environment.

---

# 2. Spring Boot Configuration

- [ ] Spring Web is configured.
- [ ] Spring Data JPA is configured.
- [ ] PostgreSQL driver is configured.
- [ ] Flyway is configured.
- [ ] Application configuration is externalized through `application.yml` / environment variables where appropriate.
- [ ] Application starts successfully with PostgreSQL available.

---

# 3. Database

- [ ] PostgreSQL database is available locally.
- [ ] Flyway manages database schema changes.
- [ ] Initial migration creates the `documents` table.
- [ ] Initial migration creates the `document_chunks` table.
- [ ] `documents.id` is a UUID primary key.
- [ ] `document_chunks.id` is a UUID primary key.
- [ ] `document_chunks.document_id` references `documents.id`.
- [ ] Foreign key uses `ON DELETE CASCADE`.
- [ ] `(document_id, chunk_index)` is unique.
- [ ] Document processing status is persisted.
- [ ] Database schema matches `docs/database.md`.

---

# 4. Document Domain

- [ ] `Document` domain/entity is implemented.
- [ ] `DocumentChunk` domain/entity is implemented.
- [ ] Document statuses are represented as an enum.
- [ ] Document metadata is persisted correctly.
- [ ] Document chunks are persisted correctly.
- [ ] A document can have multiple chunks.
- [ ] Deleting a document removes its associated chunks.

---

# 5. Document Storage

- [ ] `DocumentStorage` abstraction is implemented.
- [ ] Local filesystem implementation is implemented.
- [ ] Uploaded PDF files are stored outside PostgreSQL.
- [ ] Each stored document has a unique storage location.
- [ ] The database stores the document storage path/key.
- [ ] Original PDF files are preserved after processing.
- [ ] Stored files can be deleted when the document is deleted.

Expected Stage 1 storage structure:

```text
./storage/documents/{document-id}.pdf
````

---

# 6. PDF Processing

* [ ] PDF files are accepted by the upload API.
* [ ] Non-PDF files are rejected.
* [ ] PDF text can be extracted successfully.
* [ ] PDF extraction logic is separated from the Document Service.
* [ ] Extracted text is passed to the chunking component.
* [ ] Empty or unreadable documents are handled appropriately.

---

# 7. Text Chunking

* [ ] `TextChunker` abstraction is implemented.
* [ ] Extracted text is split into multiple chunks when appropriate.
* [ ] Chunks preserve their original order.
* [ ] Each chunk receives a zero-based `chunk_index`.
* [ ] Chunk content is persisted in PostgreSQL.
* [ ] Chunking logic is independent from PDF extraction.
* [ ] Chunking behavior is covered by unit tests.

The initial chunking strategy should remain simple.

Advanced semantic chunking is not required for Stage 1.

---

# 8. REST API

The following endpoints must be implemented:

```text
POST   /documents
GET    /documents
GET    /documents/{id}
DELETE /documents/{id}
```

### POST `/documents`

* [ ] Accepts `multipart/form-data`.
* [ ] Accepts a `file` parameter.
* [ ] Rejects missing files.
* [ ] Rejects unsupported file types.
* [ ] Stores the original PDF.
* [ ] Extracts text.
* [ ] Creates chunks.
* [ ] Persists metadata and chunks.
* [ ] Returns `201 Created` on success.
* [ ] Response matches `docs/api.md`.

### GET `/documents`

* [ ] Returns all documents.
* [ ] Returns document metadata.
* [ ] Does not return complete chunk content.
* [ ] Returns `200 OK`.

### GET `/documents/{id}`

* [ ] Returns the requested document.
* [ ] Returns `404 Not Found` if the document does not exist.
* [ ] Returns chunk count.
* [ ] Returns `200 OK` when successful.

### DELETE `/documents/{id}`

* [ ] Deletes the document.
* [ ] Deletes the associated PDF.
* [ ] Associated chunks are deleted.
* [ ] Returns `204 No Content`.
* [ ] Returns `404 Not Found` if the document does not exist.

---

# 9. Error Handling

* [ ] Global exception handling is implemented.
* [ ] Invalid requests return `400 Bad Request`.
* [ ] Missing documents return `404 Not Found`.
* [ ] Unexpected errors return `500 Internal Server Error`.
* [ ] Error responses follow a consistent structure.
* [ ] Stack traces are not exposed through the API.
* [ ] Internal implementation details are not exposed to clients.

---

# 10. Architecture

* [ ] Application follows the modular monolith approach.
* [ ] Document functionality is contained inside the `document` module.
* [ ] Controllers do not contain business logic.
* [ ] Business logic is implemented in services.
* [ ] Persistence is handled through repositories.
* [ ] Infrastructure concerns are separated from business logic.
* [ ] `DocumentStorage` is used instead of directly coupling the service to the filesystem.
* [ ] PDF extraction is separated from the Document Service.
* [ ] Text chunking is separated from PDF extraction.
* [ ] Architecture matches `docs/architecture.md`.

---

# 11. Testing

* [ ] Unit tests exist for the Document Service.
* [ ] Unit tests exist for the text chunking logic.
* [ ] Unit tests cover successful document processing.
* [ ] Unit tests cover processing failures.
* [ ] Unit tests cover invalid document scenarios.
* [ ] Repository behavior is tested where appropriate.
* [ ] REST endpoints have integration/API tests where appropriate.
* [ ] Tests can run through Maven.

The initial test stack is:

```text
JUnit 5
Mockito
Spring Boot Test
```

Testcontainers will be introduced in a later stage.

---

# 12. Documentation

* [ ] `README.md` describes the project.
* [ ] `docs/database.md` describes the database schema.
* [ ] `docs/api.md` describes the API contract.
* [ ] `docs/architecture.md` describes the architecture.
* [ ] This document defines the Stage 1 completion criteria.
* [ ] Documentation reflects the actual implementation.

---

# 13. Git

* [ ] Changes are committed regularly.
* [ ] Commit messages clearly describe the changes.
* [ ] Code and documentation are pushed to GitHub.
* [ ] Repository contains no secrets.
* [ ] Local configuration containing secrets is excluded from Git.

---

# 14. Manual End-to-End Verification

The complete application flow must work locally.

Starting from a clean application:

```text
Start Application
       │
       ▼
POST PDF
       │
       ▼
PDF stored locally
       │
       ▼
Text extracted
       │
       ▼
Text chunked
       │
       ▼
Document saved
       │
       ▼
Chunks saved
       │
       ▼
GET /documents
       │
       ▼
Document is visible
       │
       ▼
GET /documents/{id}
       │
       ▼
Correct metadata and chunk count
       │
       ▼
DELETE /documents/{id}
       │
       ▼
Document removed
       │
       ▼
PDF removed
       │
       ▼
Chunks removed
```

---

# Stage 1 Completion Criteria

Stage 1 is considered complete when:

1. The application starts successfully.
2. PostgreSQL schema is created through Flyway.
3. A PDF can be uploaded through the REST API.
4. The original PDF is stored locally.
5. Text is extracted from the PDF.
6. Extracted text is split into chunks.
7. Document metadata is stored in PostgreSQL.
8. Document chunks are stored in PostgreSQL.
9. Documents can be listed.
10. Individual documents can be retrieved.
11. Documents can be deleted.
12. Error scenarios are handled consistently.
13. Automated tests pass.
14. The implementation matches the documented architecture, database schema, and API contract.
15. The project can be built and run locally from the GitHub repository.

---

# Explicitly Out of Scope

The following are intentionally excluded from Stage 1:

```text
AWS
Docker
Amazon S3
Amazon ECS / Fargate
Amazon SQS
Terraform
CI/CD
LLM integration
Embeddings
Vector search
pgvector
RAG
Authentication
Frontend
Microservices
Advanced document formats
```

These capabilities belong to future stages.

---

# Stage 1 Exit Condition

When all required checklist items are complete and the end-to-end flow works successfully, the project can move to the next stage.

The next stage should be defined only after Stage 1 is completed and the current implementation has been evaluated.
