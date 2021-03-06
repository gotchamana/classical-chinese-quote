-- DROP TABLE IF EXISTS section;
-- DROP TABLE IF EXISTS book;

-- DROP ALIAS INIT_DATA IF EXISTS;

CREATE TABLE IF NOT EXISTS book (
    id IDENTITY,
    title VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS section (
    id IDENTITY,
    urn VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(50) NOT NULL,
    book_id BIGINT NOT NULL,
    FOREIGN KEY(book_id) REFERENCES book(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS paragraph (
    id IDENTITY,
    section_id BIGINT NOT NULL,
    content VARCHAR(1024) NOT NULL,
    FOREIGN KEY(section_id) REFERENCES section(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE ALIAS IF NOT EXISTS INIT_DATA FOR "io.github.repository.StoredProcedureKt.initData";

CALL INIT_DATA();