````md id="48317"
# API Contract

## Overview

The API provides endpoints for uploading, retrieving, listing, and deleting documents.

All endpoints are exposed under the `/documents` resource.

Stage 1 uses synchronous document processing.

Client
   │
   ▼
POST /documents
   │
   ├── Store PDF
   ├── Extract text
   ├── Split text into chunks
   └── Persist metadata and chunks
   │
   ▼
HTTP Response
````

No frontend is required for Stage 1. The API can be used with tools such as:

* IntelliJ HTTP Client
* Postman
* cURL

---

# Base URL

For local development:

```text
http://localhost:8080
```

---

# Endpoints

| Method   | Endpoint          | Description                       |
| -------- | ----------------- | --------------------------------- |
| `POST`   | `/documents`      | Upload and process a PDF document |
| `GET`    | `/documents`      | Retrieve all documents            |
| `GET`    | `/documents/{id}` | Retrieve a specific document      |
| `DELETE` | `/documents/{id}` | Delete a document                 |

---

# POST `/documents`

Uploads a PDF document and processes it synchronously.

## Request

```http
POST /documents
Content-Type: multipart/form-data
```

### Form Parameters

| Parameter | Type           | Required | Description            |
| --------- | -------------- | -------: | ---------------------- |
| `file`    | Multipart File |      Yes | PDF document to upload |

### Example

```bash
curl -X POST http://localhost:8080/documents \
  -F "file=@aws.pdf"
```

---

## Successful Response

### HTTP `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "aws.pdf",
  "contentType": "application/pdf",
  "fileSize": 245678,
  "status": "PROCESSED",
  "createdAt": "2026-09-10T15:30:00Z"
}
```

### Response Fields

| Field         | Type     | Description                 |
| ------------- | -------- | --------------------------- |
| `id`          | UUID     | Unique document identifier  |
| `fileName`    | String   | Original file name          |
| `contentType` | String   | MIME type                   |
| `fileSize`    | Long     | File size in bytes          |
| `status`      | String   | Current processing status   |
| `createdAt`   | DateTime | Document creation timestamp |

---

## Processing Behavior

The upload request is processed synchronously in Stage 1.

```text
Upload
  │
  ▼
Store original PDF
  │
  ▼
Extract text
  │
  ▼
Split text into chunks
  │
  ▼
Persist document and chunks
  │
  ▼
Return response
```

If processing succeeds:

```text
HTTP 201 Created
status = PROCESSED
```

If processing fails:

```text
HTTP 500 Internal Server Error
status = FAILED
```

Asynchronous processing using Amazon SQS will be introduced in a later stage.

---

# GET `/documents`

Returns all documents known to the system.

## Request

```http
GET /documents
```

No request body is required.

---

## Successful Response

### HTTP `200 OK`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "aws.pdf",
    "contentType": "application/pdf",
    "fileSize": 245678,
    "status": "PROCESSED",
    "createdAt": "2026-09-10T15:30:00Z"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440111",
    "fileName": "spring.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "status": "PROCESSED",
    "createdAt": "2026-09-10T15:35:00Z"
  }
]
```

The endpoint returns document metadata only.

Document chunks are not included in the list response.

---

# GET `/documents/{id}`

Returns information about a specific document.

## Path Parameters

| Parameter | Type | Required | Description         |
| --------- | ---- | -------: | ------------------- |
| `id`      | UUID |      Yes | Document identifier |

### Example

```http
GET /documents/550e8400-e29b-41d4-a716-446655440000
```

---

## Successful Response

### HTTP `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "aws.pdf",
  "contentType": "application/pdf",
  "fileSize": 245678,
  "status": "PROCESSED",
  "createdAt": "2026-09-10T15:30:00Z",
  "updatedAt": "2026-09-10T15:30:02Z",
  "chunkCount": 42
}
```

### Response Fields

| Field         | Type     | Description                     |
| ------------- | -------- | ------------------------------- |
| `id`          | UUID     | Unique document identifier      |
| `fileName`    | String   | Original file name              |
| `contentType` | String   | MIME type                       |
| `fileSize`    | Long     | File size in bytes              |
| `status`      | String   | Current processing status       |
| `createdAt`   | DateTime | Document creation timestamp     |
| `updatedAt`   | DateTime | Last update timestamp           |
| `chunkCount`  | Integer  | Number of generated text chunks |

The chunk content itself is not returned by this endpoint.

---

# DELETE `/documents/{id}`

Deletes a document and its associated data.

## Path Parameters

| Parameter | Type | Required | Description         |
| --------- | ---- | -------: | ------------------- |
| `id`      | UUID |      Yes | Document identifier |

### Example

```http
DELETE /documents/550e8400-e29b-41d4-a716-446655440000
```

---

## Successful Response

### HTTP `204 No Content`

The response contains no body.

When a document is deleted:

```text
Document
   │
   ├── Delete original PDF
   │
   └── Delete database record
          │
          └── Cascade delete chunks
```

---

# Error Responses

The API uses standard HTTP status codes.

## `400 Bad Request`

Returned when the request is invalid.

Examples:

* No file provided
* Invalid request parameters
* Unsupported file type

Example:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "A PDF file is required",
  "timestamp": "2026-09-10T15:30:00Z"
}
```

---

## `404 Not Found`

Returned when the requested document does not exist.

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Document not found",
  "timestamp": "2026-09-10T15:30:00Z"
}
```

---

## `500 Internal Server Error`

Returned when an unexpected server-side error occurs.

Example:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Document processing failed",
  "timestamp": "2026-09-10T15:30:00Z"
}
```

Internal implementation details and stack traces must not be exposed through the API.

---

# Supported File Types

Stage 1 supports PDF documents only.

```text
application/pdf
```

Other document formats are not supported.

Future versions may support additional formats such as:

```text
DOCX
TXT
HTML
```

---

# API Design Decisions

## Synchronous Processing

Document processing is synchronous in Stage 1 because the expected documents are small.

This keeps the initial implementation simple and makes the processing flow easy to understand and test.

Future architecture:

```text
Stage 1

POST /documents
      │
      ▼
Process document synchronously
      │
      ▼
Return response


Future

POST /documents
      │
      ▼
Store document
      │
      ▼
Publish message to SQS
      │
      ▼
Return response
      │
      │
      ▼
   Worker
      │
      ├── Extract text
      ├── Chunk
      └── Generate embeddings
```

The API contract should remain as stable as possible while the internal processing architecture evolves.

---

# No Frontend

Stage 1 does not include a frontend application.

The API is the primary interface and can be tested using:

```text
IntelliJ HTTP Client
Postman
cURL
```

---

# Future API Extensions

The following endpoints are intentionally not part of Stage 1.

## Search

```text
GET /search
```

Will be introduced when document retrieval is implemented.

## Questions

```text
POST /questions
```

Will be introduced when RAG and LLM-based question answering are implemented.

## Chunk Retrieval

Potential future endpoint:

```text
GET /documents/{id}/chunks
```

This is not required for the initial version.

---

# API Versioning

API versioning is intentionally omitted from Stage 1.

The initial API uses:

```text
/documents
```

rather than:

```text
/api/v1/documents
```

Versioning can be introduced when there is a real need to support multiple API versions.
