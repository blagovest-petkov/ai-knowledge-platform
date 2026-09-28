# Stage 1 Tasks

## Goal

Build the first working version of the AI Knowledge Platform as a modular monolith.

Stage 1 includes:

- PDF upload
- Local PDF storage
- PDF text extraction
- Text chunking
- PostgreSQL persistence
- REST API
- Synchronous processing
- Unit and integration tests

Stage 1 does **not** include:

- AWS
- S3
- Docker
- SQS
- Terraform
- CI/CD
- LLM
- Embeddings
- Vector search
- pgvector
- RAG
- Authentication
- Frontend
- Microservices

---

# 1. Project Setup

## 1.1 Create Spring Boot project

- [ ] Create Spring Boot project
- [ ] Use Java 21
- [ ] Use Maven
- [ ] Use JAR packaging
- [ ] Choose project name: `ai-knowledge-platform`

## 1.2 Configure Java

- [ ] Configure Java 21
- [ ] Verify Maven uses Java 21
- [ ] Verify IntelliJ uses Java 21

## 1.3 Configure Maven dependencies

Required dependencies:

- [ ] Spring Web
- [ ] Spring Data JPA
- [ ] PostgreSQL Driver
- [ ] Flyway
- [ ] Validation
- [ ] JUnit 5
- [ ] Mockito

## 1.4 Configure application

- [ ] Create `application.yml`
- [ ] Configure application name
- [ ] Configure PostgreSQL connection
- [ ] Configure JPA
- [ ] Configure Flyway
- [ ] Configure local document storage path
- [ ] Keep environment-specific values externalizable
- [ ] Do not commit passwords or secrets

## 1.5 Verify application starts

- [ ] Start Spring Boot application
- [ ] Verify application starts without errors
- [ ] Verify database connection configuration
- [ ] Verify Flyway starts correctly

## 1.6 Create initial project structure

Create the initial modular-monolith structure:

```text
document/
    controller/
    service/
    repository/
    model/

search/
    controller/
    service/

question/
    controller/
    service/

common/
````

* [ ] Create package structure
* [ ] Keep business logic inside the appropriate domain
* [ ] Avoid global `controller/service/repository` packages

## 1.7 Initial Git commit

* [ ] Verify `.gitignore`
* [ ] Add project files
* [ ] Commit initial project
* [ ] Push to GitHub

---

# 2. Database

## 2.1 Set up local PostgreSQL

* [ ] Install PostgreSQL if necessary
* [ ] Create local database
* [ ] Create local database user
* [ ] Verify connection

## 2.2 Configure Flyway

* [ ] Add Flyway dependency
* [ ] Create migrations directory
* [ ] Configure Flyway
* [ ] Verify migrations execute automatically

## 2.3 Create `documents` migration

Create:

```text
documents
```

Fields:

* `id UUID PRIMARY KEY`
* `file_name VARCHAR(255) NOT NULL`
* `content_type VARCHAR(100) NOT NULL`
* `file_size BIGINT NOT NULL`
* `status VARCHAR(30) NOT NULL`
* `storage_path VARCHAR(500) NOT NULL`
* `created_at TIMESTAMP NOT NULL`
* `updated_at TIMESTAMP NOT NULL`

Statuses:

* `UPLOADED`

* `PROCESSING`

* `PROCESSED`

* `FAILED`

* [ ] Create migration

* [ ] Run migration

* [ ] Verify table

## 2.4 Create `document_chunks` migration

Create:

```text
document_chunks
```

Fields:

* `id UUID PRIMARY KEY`
* `document_id UUID NOT NULL`
* `chunk_index INTEGER NOT NULL`
* `content TEXT NOT NULL`
* `created_at TIMESTAMP NOT NULL`

Constraints:

* [ ] Foreign key to `documents`
* [ ] `ON DELETE CASCADE`
* [ ] Unique `(document_id, chunk_index)`

## 2.5 Verify database schema

* [ ] Verify `documents`
* [ ] Verify `document_chunks`
* [ ] Verify primary keys
* [ ] Verify foreign key
* [ ] Verify cascade delete
* [ ] Verify unique constraint

## 2.6 Create `Document` entity

* [ ] Create entity
* [ ] Map UUID ID
* [ ] Map all database fields
* [ ] Add status enum
* [ ] Map timestamps

## 2.7 Create `DocumentChunk` entity

* [ ] Create entity
* [ ] Map UUID ID
* [ ] Map `document_id`
* [ ] Map `chunk_index`
* [ ] Map `content`
* [ ] Map timestamp
* [ ] Configure relationship to `Document`

## 2.8 Create `DocumentRepository`

* [ ] Extend Spring Data repository
* [ ] Verify basic CRUD

## 2.9 Create `DocumentChunkRepository`

* [ ] Extend Spring Data repository
* [ ] Add query methods if necessary
* [ ] Verify basic CRUD

## 2.10 Verify entity relationships

* [ ] Verify document → chunks relationship
* [ ] Verify cascade behavior
* [ ] Verify chunk ordering

## 2.11 Database tests

* [ ] Test document persistence
* [ ] Test chunk persistence
* [ ] Test document/chunk relationship
* [ ] Test cascade deletion
* [ ] Test unique chunk index

---

# 3. Document Storage

## 3.1 Create `DocumentStorage` interface

Define operations for:

* [ ] Store document
* [ ] Retrieve document
* [ ] Delete document

The business layer must depend on this interface rather than directly on the filesystem.

## 3.2 Configure storage

* [ ] Add configurable local storage path
* [ ] Default to something like:

```text
./storage/documents
```

## 3.3 Implement `LocalDocumentStorage`

* [ ] Create implementation
* [ ] Use Java filesystem APIs
* [ ] Create storage directory if necessary

## 3.4 Implement document storage

* [ ] Store uploaded PDF
* [ ] Generate UUID-based storage path
* [ ] Do not trust the original filename as the storage filename

Example:

```text
storage/documents/{document-id}.pdf
```

## 3.5 Implement document retrieval

* [ ] Retrieve stored document
* [ ] Handle missing file

## 3.6 Implement document deletion

* [ ] Delete stored document
* [ ] Handle already-missing file appropriately

## 3.7 Storage tests

* [ ] Store file
* [ ] Retrieve file
* [ ] Delete file
* [ ] Handle missing file
* [ ] Verify UUID-based path

## 3.8 Verify UUID-based storage

* [ ] Verify original filename is stored as metadata
* [ ] Verify physical file uses UUID
* [ ] Verify files cannot overwrite each other accidentally

---

# 4. PDF Processing

## 4.1 Choose PDF library

* [ ] Select PDF text extraction library
* [ ] Prefer a mature Java library
* [ ] Keep extraction behind an interface

## 4.2 Add PDF dependency

* [ ] Add selected library to Maven
* [ ] Verify application starts

## 4.3 Create `DocumentTextExtractor`

Define an abstraction such as:

```text
extract(file) -> text
```

* [ ] Create interface
* [ ] Keep PDF-specific implementation details outside business logic

## 4.4 Implement `PdfDocumentTextExtractor`

* [ ] Read PDF
* [ ] Extract text
* [ ] Return normalized text

## 4.5 Extract text from a real PDF

* [ ] Test with a real PDF
* [ ] Verify extracted text
* [ ] Verify multi-page documents

## 4.6 Handle invalid PDFs

* [ ] Detect unreadable PDFs
* [ ] Return meaningful application error
* [ ] Ensure processing failure is persisted appropriately

## 4.7 Handle PDFs with no useful text

* [ ] Detect empty extraction
* [ ] Decide expected behavior
* [ ] Prevent creation of meaningless chunks

## 4.8 PDF extraction tests

* [ ] Valid PDF
* [ ] Multi-page PDF
* [ ] Invalid PDF
* [ ] Empty/no-text PDF
* [ ] Extraction failure

---

# 5. Text Chunking

## 5.1 Create `TextChunker`

Define an abstraction:

```text
chunk(text) -> ordered chunks
```

* [ ] Create interface
* [ ] Keep chunking logic independent from persistence

## 5.2 Decide initial chunking strategy

For Stage 1 use a simple deterministic strategy.

Example:

* character-based chunk size
* optional overlap
* preserve original text order

Do not implement token-aware chunking or semantic chunking yet.

* [ ] Decide chunk size
* [ ] Decide overlap
* [ ] Document the decision

## 5.3 Implement chunking

* [ ] Split text
* [ ] Preserve content
* [ ] Preserve order
* [ ] Avoid losing text between chunks

## 5.4 Preserve order

* [ ] First chunk comes first
* [ ] Last chunk comes last
* [ ] No chunks are skipped

## 5.5 Use zero-based `chunk_index`

Example:

```text
0
1
2
3
...
```

* [ ] Verify indexes are sequential
* [ ] Verify no duplicates

## 5.6 Persist chunks

* [ ] Create `DocumentChunk` objects
* [ ] Associate each chunk with document
* [ ] Persist chunks
* [ ] Preserve chunk order

## 5.7 Chunker tests

* [ ] Short text
* [ ] Text smaller than chunk size
* [ ] Text exactly at chunk size
* [ ] Large text
* [ ] Text requiring multiple chunks
* [ ] Verify order

## 5.8 Edge cases

* [ ] Empty text
* [ ] Whitespace-only text
* [ ] Very large text
* [ ] Special characters
* [ ] Newlines

---

# 6. Document Service

## 6.1 Create document service

Create the main application service responsible for document processing.

The service should coordinate:

```text
Storage
    ↓
Text extraction
    ↓
Chunking
    ↓
Persistence
```

## 6.2 Implement upload flow

Expected flow:

```text
Upload PDF
    ↓
Create document metadata
    ↓
Store PDF
    ↓
Extract text
    ↓
Chunk text
    ↓
Persist chunks
    ↓
Mark document PROCESSED
```

* [ ] Implement complete flow
* [ ] Keep HTTP-specific types out of the service

## 6.3 Implement document status

Use:

```text
UPLOADED
PROCESSING
PROCESSED
FAILED
```

* [ ] Set initial status
* [ ] Set `PROCESSING`
* [ ] Set `PROCESSED`
* [ ] Set `FAILED` when processing fails

## 6.4 Store document metadata

Persist:

* [ ] Original filename
* [ ] Content type
* [ ] File size
* [ ] Storage path
* [ ] Status
* [ ] Timestamps

## 6.5 Store PDF

* [ ] Call `DocumentStorage`
* [ ] Store original PDF
* [ ] Keep storage implementation hidden behind interface

## 6.6 Extract text

* [ ] Call `DocumentTextExtractor`
* [ ] Handle extraction failures

## 6.7 Create chunks

* [ ] Call `TextChunker`
* [ ] Create ordered chunks

## 6.8 Persist document and chunks

* [ ] Persist document
* [ ] Persist chunks
* [ ] Ensure transaction boundaries are explicit

Important:

Database transactions and filesystem operations are not one atomic transaction.

* [ ] Define behavior if DB persistence fails after file storage
* [ ] Define behavior if extraction fails
* [ ] Define behavior if chunk persistence fails

## 6.9 Retrieve document

* [ ] Retrieve document metadata
* [ ] Return chunk count
* [ ] Return 404 when document does not exist

## 6.10 List documents

* [ ] Return document metadata
* [ ] Do not return full PDF content
* [ ] Do not return all chunks

## 6.11 Delete document

Delete:

1. Stored PDF
2. Database document
3. Associated chunks

* [ ] Verify cascade deletion
* [ ] Verify physical file deletion
* [ ] Return appropriate result if document does not exist

## 6.12 Handle processing failures

* [ ] Mark document as `FAILED`
* [ ] Preserve useful error information in logs
* [ ] Avoid leaving misleading `PROCESSED` records
* [ ] Consider cleanup of partially stored files

## 6.13 Service unit tests

Test:

* [ ] Successful upload
* [ ] Storage failure
* [ ] Extraction failure
* [ ] Chunking failure
* [ ] Database failure
* [ ] Retrieve document
* [ ] List documents
* [ ] Delete document

Use Mockito where appropriate.

---

# 7. REST API

## 7.1 Create document controller

* [ ] Create `DocumentController`
* [ ] Keep controller thin
* [ ] Delegate business logic to service

## 7.2 Implement `POST /documents`

Request:

```text
multipart/form-data
file=<PDF>
```

Requirements:

* [ ] Accept PDF upload
* [ ] Validate content
* [ ] Call document service
* [ ] Return document metadata
* [ ] Return `201 Created` on success

## 7.3 Implement `GET /documents`

* [ ] Return list of documents
* [ ] Return metadata only
* [ ] Return `200 OK`

## 7.4 Implement `GET /documents/{id}`

* [ ] Return document metadata
* [ ] Return chunk count
* [ ] Return `404 Not Found` if missing

## 7.5 Implement `DELETE /documents/{id}`

* [ ] Delete document
* [ ] Delete stored PDF
* [ ] Delete chunks
* [ ] Return `204 No Content`
* [ ] Return `404 Not Found` if missing

## 7.6 Add validation

* [ ] Require file
* [ ] Reject unsupported content types
* [ ] Reject invalid uploads
* [ ] Define sensible file-size limits if appropriate

Stage 1 supports:

```text
application/pdf
```

only.

## 7.7 Create DTOs

Avoid exposing JPA entities directly from controllers.

Create DTOs for:

* [ ] Document response
* [ ] Document details
* [ ] Error response

## 7.8 Create error model

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Document not found",
  "timestamp": "2026-09-10T15:30:00Z"
}
```

* [ ] Define error DTO
* [ ] Keep error responses consistent

## 7.9 Global exception handling

* [ ] Create `@RestControllerAdvice`
* [ ] Handle validation errors
* [ ] Handle document-not-found errors
* [ ] Handle invalid PDF errors
* [ ] Handle processing errors
* [ ] Handle unexpected errors

## 7.10 Verify HTTP status codes

Expected:

```text
POST /documents
    201 Created

GET /documents
    200 OK

GET /documents/{id}
    200 OK
    404 Not Found

DELETE /documents/{id}
    204 No Content
    404 Not Found

Invalid request
    400 Bad Request

Processing failure
    500 Internal Server Error
```

## 7.11 Match API documentation

* [ ] Compare implementation with `docs/api.md`
* [ ] Update implementation or documentation where necessary
* [ ] Do not allow documentation and implementation to diverge

---

# 8. Testing

## 8.1 Service tests

* [ ] Successful document processing
* [ ] Storage failure
* [ ] Extraction failure
* [ ] Chunking failure
* [ ] Persistence failure
* [ ] Delete flow

## 8.2 Chunker tests

* [ ] Small text
* [ ] Large text
* [ ] Boundary conditions
* [ ] Ordering
* [ ] Empty input

## 8.3 PDF extraction tests

* [ ] Valid PDF
* [ ] Multi-page PDF
* [ ] Invalid PDF
* [ ] Empty PDF

## 8.4 Storage tests

* [ ] Store
* [ ] Retrieve
* [ ] Delete
* [ ] Missing file
* [ ] UUID path

## 8.5 Repository tests

* [ ] Document persistence
* [ ] Chunk persistence
* [ ] Relationship
* [ ] Cascade delete
* [ ] Unique constraint

## 8.6 API/integration tests

Test the application through HTTP where practical.

* [ ] Upload document
* [ ] List documents
* [ ] Get document
* [ ] Delete document

## 8.7 Test successful upload

Verify:

* [ ] HTTP response
* [ ] Document record
* [ ] PDF exists
* [ ] Extracted chunks exist
* [ ] Status is `PROCESSED`

## 8.8 Test invalid file

* [ ] Upload non-PDF
* [ ] Verify rejection
* [ ] Verify correct HTTP status
* [ ] Verify no invalid document remains

## 8.9 Test missing document

* [ ] Request unknown UUID
* [ ] Verify `404`

## 8.10 Test deletion

* [ ] Upload document
* [ ] Delete document
* [ ] Verify DB record removed
* [ ] Verify chunks removed
* [ ] Verify PDF removed

## 8.11 Test processing failure

* [ ] Trigger extraction failure
* [ ] Verify status
* [ ] Verify HTTP response
* [ ] Verify cleanup behavior

## 8.12 Run full test suite

* [ ] Run `mvn test`
* [ ] Verify all tests pass
* [ ] Fix failures
* [ ] Remove flaky tests

---

# 9. Documentation & Cleanup

## 9.1 Verify database documentation

* [ ] `docs/database.md` matches actual schema
* [ ] Update documentation if implementation changed

## 9.2 Verify API documentation

* [ ] `docs/api.md` matches actual API
* [ ] Verify request examples
* [ ] Verify response examples
* [ ] Verify status codes

## 9.3 Verify architecture documentation

* [ ] `docs/architecture.md` matches actual implementation
* [ ] Verify package/module structure
* [ ] Verify storage abstraction
* [ ] Verify processing flow

## 9.4 Update README

README should explain:

* [ ] What the project does
* [ ] Current Stage
* [ ] Architecture
* [ ] Technology stack
* [ ] How to run locally
* [ ] How to run tests
* [ ] API overview
* [ ] Future roadmap

## 9.5 Review project structure

Verify that:

* [ ] Business domains are clearly separated
* [ ] Controllers are thin
* [ ] Services contain business logic
* [ ] Repositories handle persistence
* [ ] Infrastructure abstractions are respected
* [ ] No unnecessary abstractions exist

## 9.6 Clean up code

* [ ] Remove unused classes
* [ ] Remove unused dependencies
* [ ] Remove dead code
* [ ] Remove debug logging
* [ ] Check naming
* [ ] Check package structure

## 9.7 Review configuration

* [ ] Verify `application.yml`
* [ ] Verify local configuration
* [ ] Verify database configuration
* [ ] Verify storage path
* [ ] Verify no hardcoded environment-specific secrets

## 9.8 Verify secrets

* [ ] No passwords in Git
* [ ] No API keys in Git
* [ ] No tokens in Git
* [ ] `.gitignore` is correct

## 9.9 Verify local run

From a clean environment:

* [ ] Start PostgreSQL
* [ ] Start application
* [ ] Verify Flyway migrations
* [ ] Upload PDF
* [ ] Verify processing
* [ ] Retrieve document
* [ ] Delete document

## 9.10 Final end-to-end verification

Perform the complete flow:

```text
Start PostgreSQL
      ↓
Start application
      ↓
Upload PDF
      ↓
PDF stored locally
      ↓
Text extracted
      ↓
Text chunked
      ↓
Document persisted
      ↓
Chunks persisted
      ↓
GET document
      ↓
Verify chunk count
      ↓
DELETE document
      ↓
Verify DB cleanup
      ↓
Verify PDF cleanup
```

* [ ] Complete end-to-end flow successfully

## 9.11 Final Git commit

* [ ] Review `git status`
* [ ] Review changes
* [ ] Commit Stage 1 implementation
* [ ] Use a meaningful commit message

Example:

```text
Complete Stage 1 document processing pipeline
```

## 9.12 Push to GitHub

* [ ] Push final Stage 1 implementation
* [ ] Verify repository contents
* [ ] Verify README
* [ ] Verify documentation

---

# Stage 1 Final Verification

Before considering Stage 1 complete, verify:

* [ ] Application starts
* [ ] PostgreSQL works
* [ ] Flyway migrations work
* [ ] Documents can be uploaded
* [ ] Only PDFs are accepted
* [ ] Original PDFs are stored locally
* [ ] PDF text is extracted
* [ ] Text is split into chunks
* [ ] Chunk order is preserved
* [ ] Documents are persisted
* [ ] Chunks are persisted
* [ ] Document status is maintained
* [ ] Documents can be listed
* [ ] Documents can be retrieved
* [ ] Documents can be deleted
* [ ] PDFs are deleted correctly
* [ ] Chunks are deleted correctly
* [ ] Errors are handled consistently
* [ ] Unit tests pass
* [ ] Integration tests pass
* [ ] End-to-end flow works
* [ ] Documentation matches implementation
* [ ] No secrets are committed
* [ ] Code structure follows the modular-monolith architecture
* [ ] GitHub repository is up to date

---

# Stage 1 Complete

Stage 1 is complete when the application can reliably perform:

```text
PDF
 ↓
Upload
 ↓
Local Storage
 ↓
Text Extraction
 ↓
Chunking
 ↓
PostgreSQL
 ↓
REST API
```

and the complete flow is covered by tests and documented.

Only after this is stable should the project move to:

```text
Stage 2 → Docker
Stage 3 → AWS
Stage 4 → SQS / Async Processing
Stage 5 → AI / RAG
Stage 6 → Terraform
Stage 7 → CI/CD + Observability
```

```
```
