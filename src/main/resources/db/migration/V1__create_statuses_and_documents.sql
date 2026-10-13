CREATE TABLE statuses
(
    status VARCHAR(30) PRIMARY KEY
);

INSERT INTO statuses (status)
VALUES ('UPLOADED'),
       ('PROCESSING'),
       ('PROCESSED'),
       ('FAILED');


CREATE TABLE documents
(
    id           UUID PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT       NOT NULL,
    status       VARCHAR(30)  NOT NULL REFERENCES statuses (status),
    storage_path VARCHAR(500) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);