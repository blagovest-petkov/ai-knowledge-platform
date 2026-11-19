package com.example.ai_knowledge_platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DocumentChunksMigrationIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void insertsChunkForExistingDocument() {
        UUID documentId = insertDocument();

        insertChunk(documentId, 0);
        insertChunk(documentId, 1);

        assertThat(countChunks(documentId)).isEqualTo(2);
    }

    @Test
    void allowsSameChunkIndexAcrossDifferentDocuments() {
        UUID firstDocumentId = insertDocument();
        UUID secondDocumentId = insertDocument();

        insertChunk(firstDocumentId, 0);
        insertChunk(secondDocumentId, 0);

        assertThat(countChunks(firstDocumentId)).isEqualTo(1);
        assertThat(countChunks(secondDocumentId)).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateChunkIndexWithinDocument() {
        UUID documentId = insertDocument();
        insertChunk(documentId, 0);

        assertThatThrownBy(() -> insertChunk(documentId, 0))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_document_chunk_index");
    }

    @Test
    void rejectsChunkForMissingDocument() {
        assertThatThrownBy(() -> insertChunk(UUID.randomUUID(), 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_document_chunks_document");
    }

    @Test
    void rejectsNegativeChunkIndex() {
        UUID documentId = insertDocument();

        assertThatThrownBy(() -> insertChunk(documentId, -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_document_chunks_chunk_index_non_negative");
    }

    @Test
    void deletingDocumentCascadesToChunks() {
        UUID documentId = insertDocument();
        insertChunk(documentId, 0);
        insertChunk(documentId, 1);

        jdbcTemplate.update("DELETE FROM documents WHERE id = ?", documentId);

        assertThat(countChunks(documentId)).isZero();
    }

    private UUID insertDocument() {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                """
                INSERT INTO documents
                    (id, file_name, content_type, file_size, status, storage_path, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "test.pdf",
                "application/pdf",
                1024L,
                "UPLOADED",
                "./uploads/" + id + ".pdf",
                now,
                now);
        return id;
    }

    private void insertChunk(UUID documentId, int chunkIndex) {
        jdbcTemplate.update(
                """
                INSERT INTO document_chunks (id, document_id, chunk_index, content, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                documentId,
                chunkIndex,
                "chunk " + chunkIndex,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    private int countChunks(UUID documentId) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
                        Integer.class,
                        documentId);
        return count == null ? 0 : count;
    }
}
