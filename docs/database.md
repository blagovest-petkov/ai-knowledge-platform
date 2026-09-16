````md
# Database Schema

## Overview

The application uses PostgreSQL as its relational database.

The database stores document metadata and the text chunks extracted from uploaded PDF documents.

The original PDF files are **not stored in PostgreSQL**. They are stored using the configured `DocumentStorage` implementation. In Stage 1, the implementation uses the local filesystem.

### Current entities

- `documents` — stores metadata about uploaded documents.
- `document_chunks` — stores text extracted from documents and split into chunks.

### Relationship

```text
documents
    │
    │ 1:N
    ▼
document_chunks
````

A document can contain multiple chunks, while each chunk belongs to exactly one document.

---

# Entity: `documents`

Stores metadata about uploaded documents.

| Column         | Type         | Nullable | Constraints | Description                                  |
| -------------- | ------------ | -------: | ----------- | -------------------------------------------- |
| `id`           | UUID         |       No | Primary Key | Unique identifier of the document            |
| `file_name`    | VARCHAR(255) |       No |             | Original name of the uploaded file           |
| `content_type` | VARCHAR(100) |       No |             | MIME type of the uploaded file               |
| `file_size`    | BIGINT       |       No |             | File size in bytes                           |
| `status`       | VARCHAR(30)  |       No |             | Current document processing status           |
| `storage_path` | VARCHAR(500) |       No |             | Path or storage key of the original file     |
| `created_at`   | TIMESTAMP    |       No |             | Timestamp when the document was created      |
| `updated_at`   | TIMESTAMP    |       No |             | Timestamp when the document was last updated |

## Document Status

The `status` column represents the current processing state of a document.

Possible values:

| Status       | Description                                         |
| ------------ | --------------------------------------------------- |
| `UPLOADED`   | The file has been uploaded and stored               |
| `PROCESSING` | The document is currently being processed           |
| `PROCESSED`  | Text extraction and chunking completed successfully |
| `FAILED`     | Document processing failed                          |

### Processing flow

```text
UPLOADED
   │
   ▼
PROCESSING
   │
   ├──────────────► PROCESSED
   │
   └──────────────► FAILED
```

Although document processing is synchronous in Stage 1, the status is persisted in the database to support future asynchronous processing.

---

# Entity: `document_chunks`

Stores text extracted from a document and divided into smaller chunks.

| Column        | Type      | Nullable | Constraints         | Description                                          |
| ------------- | --------- | -------: | ------------------- | ---------------------------------------------------- |
| `id`          | UUID      |       No | Primary Key         | Unique identifier of the chunk                       |
| `document_id` | UUID      |       No | Foreign Key         | ID of the parent document                            |
| `chunk_index` | INTEGER   |       No | Unique per document | Zero-based position of the chunk within the document |
| `content`     | TEXT      |       No |                     | Extracted text contained in the chunk                |
| `created_at`  | TIMESTAMP |       No |                     | Timestamp when the chunk was created                 |

## Relationship

```text
document_chunks.document_id
            │
            ▼
documents.id
```

The relationship is:

```text
documents 1 ───────── N document_chunks
```

A document may have zero or more chunks.

Deleting a document also deletes all associated chunks.

---

# Constraints

## Primary Keys

Both entities use UUID primary keys:

```text
documents.id
document_chunks.id
```

## Foreign Key

`document_chunks.document_id` references `documents.id`.

```sql
FOREIGN KEY (document_id)
REFERENCES documents(id)
ON DELETE CASCADE
```

This ensures that chunks cannot exist without their parent document.

When a document is deleted, all associated chunks are automatically deleted.

## Unique Constraint

Each chunk must have a unique position within its document:

```text
UNIQUE(document_id, chunk_index)
```

This prevents duplicate chunk indexes for the same document.

Example:

```text
document A
    chunk 0
    chunk 1
    chunk 2

document B
    chunk 0
    chunk 1
```

The same `chunk_index` can therefore exist across different documents.

---

# Database Diagram

```text
┌─────────────────────────────┐
│          documents          │
├─────────────────────────────┤
│ PK id              UUID     │
│    file_name       VARCHAR  │
│    content_type    VARCHAR  │
│    file_size       BIGINT   │
│    status          VARCHAR  │
│    storage_path    VARCHAR  │
│    created_at      TIMESTAMP│
│    updated_at      TIMESTAMP│
└──────────────┬──────────────┘
               │
               │ 1:N
               │
┌──────────────▼──────────────┐
│       document_chunks       │
├─────────────────────────────┤
│ PK id              UUID     │
│ FK document_id     UUID     │
│    chunk_index     INTEGER  │
│    content         TEXT     │
│    created_at      TIMESTAMP│
└─────────────────────────────┘
```

---

# Initial SQL Schema

The following SQL represents the initial database schema for Stage 1.

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_document_chunk_index
        UNIQUE (document_id, chunk_index)
);
```

---

# Design Decisions

## PostgreSQL stores metadata, not the original PDF

The original PDF file is stored outside PostgreSQL.

In Stage 1:

```text
Local filesystem
    ./storage/documents/{document-id}.pdf
```

PostgreSQL stores the metadata and the relationship to the stored file through `storage_path`.

In a later stage, the storage implementation can be replaced with Amazon S3 without changing the database model.

```text
DocumentStorage
       │
       ├── LocalDocumentStorage   ← Stage 1
       │
       └── S3DocumentStorage      ← Future
```

## Original documents are preserved

The original PDF is retained after text extraction and chunking.

This allows the application to later provide references back to the original source document when answering questions.

## No embeddings in Stage 1

The `document_chunks` table intentionally does not contain embeddings or vector data.

Vector embeddings will be introduced when the RAG functionality is implemented.

Potential future additions may include:

```text
embedding
token_count
page_number
metadata
```

These are intentionally excluded from the initial schema.

## No user ownership in Stage 1

Documents are not associated with users yet.

Authentication and multi-user document ownership can be introduced in a later stage without unnecessarily complicating the initial implementation.

---

# Future Evolution

The schema is intentionally minimal for Stage 1.

Potential future changes include:

```text
documents
    │
    ├── document_chunks
    │       │
    │       └── embedding/vector data
    │
    └── document metadata

users
    │
    └── documents

questions
    │
    └── answers / retrieved chunks
```

The database model should evolve together with the application's business requirements rather than introducing future entities prematurely.
