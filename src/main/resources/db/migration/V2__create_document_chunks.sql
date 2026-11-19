CREATE TABLE document_chunks
(
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL, -- noqa: RF04
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_document_chunks_document
    FOREIGN KEY (document_id)
    REFERENCES documents (id)
    ON DELETE CASCADE,
    CONSTRAINT uq_document_chunk_index
    UNIQUE (document_id, chunk_index),
    CONSTRAINT ck_document_chunks_chunk_index_non_negative
    CHECK (chunk_index >= 0)
);
