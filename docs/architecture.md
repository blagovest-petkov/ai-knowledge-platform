````md
# Architecture

## Overview

AI Knowledge Platform is initially implemented as a modular monolith.

The application is designed to evolve from a simple local application into a cloud-based AI/RAG platform while keeping the initial implementation small and understandable.

The architecture follows a few core principles:

- Keep business domains separated.
- Keep infrastructure concerns behind interfaces where appropriate.
- Prefer simple solutions until complexity is justified.
- Design for future evolution without implementing future requirements prematurely.
- Keep the application testable.
- Keep dependencies between modules explicit.

---

# Current Architecture

Stage 1 uses a modular monolith architecture.

```text
┌─────────────────────────────────────────────┐
│              AI Knowledge Platform          │
│                                             │
│  ┌─────────────┐                            │
│  │  Document   │                            │
│  │   Module    │                            │
│  │             │                            │
│  │ Controller  │                            │
│  │ Service     │                            │
│  │ Repository  │                            │
│  │ Model       │                            │
│  └──────┬──────┘                            │
│         │                                   │
│         ▼                                   │
│  ┌──────────────────┐                       │
│  │ DocumentStorage  │                       │
│  └────────┬─────────┘                       │
│           │                                 │
└───────────┼─────────────────────────────────┘
            │
            ▼
      Local Filesystem

            │
            ▼

       PostgreSQL
````

Document processing is synchronous in Stage 1.

```text
Client
  │
  │ POST /documents
  ▼
Document Controller
  │
  ▼
Document Service
  │
  ├── Store PDF
  │
  ├── Extract text
  │
  ├── Split text into chunks
  │
  └── Persist metadata and chunks
  │
  ▼
HTTP Response
```

---

# Architectural Style

## Modular Monolith

The application is deployed as a single application, but its code is organized around business domains.

Initial modules:

```text
document
search
question
common
```

Not every module needs to contain functionality in Stage 1.

For example:

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
```

The `search` and `question` modules will initially contain little or no implementation. They represent future business capabilities rather than infrastructure layers.

---

# Why a Modular Monolith?

A modular monolith provides a useful middle ground between a simple layered application and a distributed microservices architecture.

It allows the project to:

* Start with a single deployable application.
* Avoid distributed-system complexity during early development.
* Keep business domains separated.
* Define clear module boundaries.
* Test modules independently.
* Introduce asynchronous processing later.
* Extract individual modules into services if there is a real reason to do so.

The goal is not to build microservices from day one.

The goal is to make future extraction possible without requiring a complete rewrite.

---

# Module Boundaries

Business modules should own their own domain logic.

For example:

```text
document/
├── controller/
├── service/
├── repository/
└── model/
```

The Document module is responsible for:

* Uploading documents.
* Validating documents.
* Storing document metadata.
* Managing document processing.
* Extracting text.
* Creating chunks.
* Deleting documents.

Future modules will have their own responsibilities.

### Search

Responsible for:

* Searching document content.
* Retrieving relevant chunks.
* Future vector similarity search.

### Question

Responsible for:

* Receiving user questions.
* Retrieving relevant context.
* Calling an LLM.
* Building the final answer.
* Returning source references.

---

# Dependency Direction

Dependencies between modules should remain explicit.

The preferred direction is:

```text
Controller
    │
    ▼
Service
    │
    ▼
Repository / Infrastructure
```

Business logic should not depend directly on HTTP-specific details.

For example, the Document Service should not depend on `HttpServletRequest`.

Instead:

```text
Controller
    │
    ▼
DocumentService
    │
    ├── DocumentRepository
    │
    ├── DocumentStorage
    │
    ├── PdfTextExtractor
    │
    └── TextChunker
```

This makes the business logic easier to test and allows infrastructure implementations to change independently.

---

# Document Storage

The application does not store the original PDF files inside PostgreSQL.

The storage mechanism is abstracted behind an interface:

```java
public interface DocumentStorage {

    void store(UUID documentId, InputStream content);

    InputStream retrieve(UUID documentId);

    void delete(UUID documentId);
}
```

Stage 1 uses a local filesystem implementation:

```text
DocumentStorage
      │
      ▼
LocalDocumentStorage
      │
      ▼
./storage/documents/{document-id}.pdf
```

The application should depend on the `DocumentStorage` abstraction rather than directly on the filesystem.

---

# Future S3 Storage

In a later AWS stage, the storage implementation can be replaced:

```text
                 DocumentStorage
                       │
             ┌─────────┴──────────┐
             ▼                    ▼
 LocalDocumentStorage    S3DocumentStorage
       Stage 1                AWS
```

The Document module should not need to know whether the document is stored locally or in Amazon S3.

This provides a practical example of dependency inversion and makes the transition to AWS easier.

---

# Document Processing

Stage 1 processes documents synchronously.

```text
PDF
 │
 ▼
DocumentStorage
 │
 ▼
PDF Text Extraction
 │
 ▼
Text Chunking
 │
 ▼
PostgreSQL
```

The original PDF is preserved.

The extracted text is divided into chunks and stored in the `document_chunks` table.

The chunks will later become the basis for semantic search and RAG.

---

# PDF Text Extraction

PDF processing is treated as an infrastructure concern.

The Document module should not contain PDF-library-specific logic directly inside the business service.

A possible abstraction is:

```java
public interface DocumentTextExtractor {

    String extract(InputStream document);
}
```

Stage 1 can provide a PDF implementation:

```text
DocumentTextExtractor
        │
        ▼
PdfDocumentTextExtractor
```

This keeps the Document Service independent from the specific PDF processing library.

---

# Text Chunking

Text chunking is separated from PDF extraction.

The responsibilities are:

```text
PDF
 │
 ▼
Text Extraction
 │
 ▼
Plain Text
 │
 ▼
Chunking
 │
 ▼
Document Chunks
```

A possible abstraction:

```java
public interface TextChunker {

    List<String> chunk(String text);
}
```

This allows the chunking strategy to evolve later without changing the document upload flow.

---

# Persistence

PostgreSQL is used as the relational database.

The database stores:

* Document metadata.
* Document processing status.
* Document chunks.

The database does not store the original PDF file.

```text
PostgreSQL
│
├── documents
│
└── document_chunks
```

Database schema details are documented in:

```text
docs/database.md
```

---

# API Layer

The application exposes a REST API.

Stage 1 endpoints:

```text
POST   /documents
GET    /documents
GET    /documents/{id}
DELETE /documents/{id}
```

The API contract is documented in:

```text
docs/api.md
```

The controller layer is responsible for:

* HTTP request handling.
* Request validation.
* Mapping requests to application/service calls.
* Mapping results to HTTP responses.

Business logic should remain outside the controllers.

---

# Error Handling

The application should use centralized exception handling.

A possible implementation:

```text
@RestControllerAdvice
```

This provides consistent error responses across the API.

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Document not found",
  "timestamp": "2026-09-10T15:30:00Z"
}
```

Internal implementation details and stack traces must not be exposed to API clients.

---

# Transaction Boundaries

Document processing contains multiple operations:

```text
1. Store PDF
2. Create document metadata
3. Extract text
4. Create chunks
5. Persist chunks
6. Mark document as PROCESSED
```

Database operations should use appropriate transaction boundaries.

File-system operations and database transactions are not treated as one atomic transaction.

Failure scenarios must therefore be handled explicitly.

For example, if processing fails after the PDF has been stored, the application should avoid leaving inconsistent database state.

The exact failure-handling strategy will be implemented and tested during Stage 1.

---

# Processing Status

Documents have a processing status:

```text
UPLOADED
    │
    ▼
PROCESSING
    │
    ├──► PROCESSED
    │
    └──► FAILED
```

Although Stage 1 is synchronous, persisting processing status prepares the domain model for asynchronous processing.

---

# Future Asynchronous Processing

In a future stage, synchronous processing will be replaced with asynchronous processing using Amazon SQS.

Current:

```text
POST /documents
      │
      ▼
Process document
      │
      ▼
Return response
```

Future:

```text
POST /documents
      │
      ▼
Store document
      │
      ▼
Publish SQS message
      │
      ▼
Return response
      │
      │
      ▼
   Worker
      │
      ├── Extract text
      ├── Chunk text
      └── Generate embeddings
```

The goal is to keep the external API relatively stable while changing the internal processing model.

---

# Future AI / RAG Architecture

AI functionality will be introduced after the document processing foundation is complete.

The expected high-level flow is:

```text
                 Document Ingestion

PDF
 │
 ▼
S3
 │
 ▼
SQS
 │
 ▼
Document Worker
 │
 ├── Text Extraction
 ├── Chunking
 └── Embeddings
       │
       ▼
   PostgreSQL
   + pgvector
```

Question answering:

```text
User Question
      │
      ▼
Question API
      │
      ▼
Question Embedding
      │
      ▼
Vector Similarity Search
      │
      ▼
Relevant Document Chunks
      │
      ▼
LLM
      │
      ▼
Answer + Sources
```

The exact AI provider and model will be decided when this stage is implemented.

---

# Future AWS Architecture

The planned AWS architecture is expected to use:

```text
                    Internet
                       │
                       ▼
                 Application API
                       │
              ┌────────┴────────┐
              ▼                 ▼
          Amazon S3          Amazon SQS
              │                 │
              │                 ▼
              │              Worker
              │                 │
              │                 ▼
              └──────────► PostgreSQL
                              │
                              ▼
                           pgvector
```

Potential AWS services:

* Amazon ECS / Fargate
* Amazon RDS for PostgreSQL
* Amazon S3
* Amazon SQS
* AWS IAM
* AWS Secrets Manager
* Amazon CloudWatch
* Amazon VPC

Services will be introduced incrementally rather than all at once.

---

# Infrastructure as Code

Terraform will be introduced in a later stage.

The goal is to manage cloud infrastructure through code instead of manually configuring resources.

Expected structure:

```text
infrastructure/
├── terraform/
│   ├── environments/
│   └── modules/
```

Infrastructure code will be kept separate from application code.

---

# CI/CD

A future stage will introduce CI/CD using GitHub Actions.

Expected pipeline:

```text
Git Push
   │
   ▼
GitHub Actions
   │
   ├── Build
   ├── Unit Tests
   ├── Integration Tests
   ├── Build Docker Image
   └── Deploy
```

The exact deployment strategy will be defined when AWS deployment is implemented.

---

# Observability

Future cloud deployment should include basic observability.

Potential components:

```text
Application
    │
    ├── Logs
    ├── Metrics
    └── Health Checks
             │
             ▼
        CloudWatch
```

Observability will be introduced when the application moves to AWS.

---

# Microservices Evolution

The application is intentionally starting as a modular monolith.

If the system eventually requires separate services, the modules provide potential extraction boundaries.

For example:

```text
Current:

AI Knowledge Platform
│
├── Document
├── Search
└── Question
```

Potential future architecture:

```text
Document Service
       │
       ▼
Search Service
       │
       ▼
Question Service
```

However, microservices should only be introduced when there is a concrete reason, such as:

* Independent scaling requirements.
* Independent deployment requirements.
* Different availability requirements.
* Clear ownership boundaries.
* Significant differences in processing characteristics.

The project should not introduce microservices simply for the sake of using microservices.

---

# Architecture Evolution

The planned evolution is:

```text
Stage 1
Modular Monolith
+ PostgreSQL
+ Local Storage
+ Synchronous Processing
        │
        ▼
Stage 2
Docker
        │
        ▼
Stage 3
AWS
+ ECS/Fargate
+ RDS
+ S3
        │
        ▼
Stage 4
Asynchronous Processing
+ SQS
+ Worker
        │
        ▼
Stage 5
AI / RAG
+ Embeddings
+ pgvector
+ LLM
        │
        ▼
Stage 6
Infrastructure as Code
+ Terraform
        │
        ▼
Stage 7
CI/CD
+ GitHub Actions
+ Observability
```

This roadmap is a direction rather than a fixed implementation plan. Architectural decisions may change as the application evolves.

---

# Core Architectural Principles

## 1. Start Simple

Do not introduce infrastructure or abstractions without a current reason.

## 2. Separate Business Domains

Organize the application around business capabilities rather than technical layers alone.

## 3. Hide Infrastructure Behind Abstractions

Examples:

```text
DocumentStorage
DocumentTextExtractor
TextChunker
```

## 4. Prefer Replaceable Implementations

Examples:

```text
LocalDocumentStorage
        ↓
S3DocumentStorage
```

and:

```text
Synchronous Processing
        ↓
SQS + Worker
```

## 5. Keep the API Stable Where Possible

Internal architecture should be allowed to evolve without unnecessarily breaking API clients.

## 6. Test Business Logic Independently

Business logic should not require a running web server or cloud infrastructure for unit testing.

## 7. Add Complexity Only When Justified

Microservices, asynchronous processing, cloud infrastructure, vector search, and AI capabilities are introduced when they provide a concrete benefit.

---

# Related Documentation

* [Database Schema](database.md)
* [API Contract](api.md)
* [Roadmap](roadmap.md)
* [Project README](../README.md)
