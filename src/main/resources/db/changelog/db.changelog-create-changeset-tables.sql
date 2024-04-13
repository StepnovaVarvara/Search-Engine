--liquibase formatted sql
--changeset Stepnova Varvara:1
CREATE TABLE site (
    id SERIAL NOT NULL PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    status_time TIMESTAMP NOT NULL,
    last_error TEXT,
    url VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

--changeset Stepnova Varvara:2
CREATE TABLE page (
    id SERIAL NOT NULL PRIMARY KEY,
    site_id INTEGER NOT NULL
            REFERENCES site (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    path TEXT NOT NULL,
    code INTEGER NOT NULL,
    content TEXT NOT NULL
);

--changeset Stepnova Varvara:3
CREATE TABLE lemma (
    id SERIAL NOT NULL PRIMARY KEY,
    site_id INTEGER NOT NULL
            REFERENCES site (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    lemma VARCHAR(255) NOT NULL,
    frequency INTEGER NOT NULL
);

--changeset Stepnova Varvara:4
CREATE TABLE index (
    id SERIAL NOT NULL PRIMARY KEY,
    page_id INTEGER NOT NULL
                REFERENCES page (id)
                ON DELETE CASCADE
                ON UPDATE CASCADE,
    lemma_id INTEGER NOT NULL
                    REFERENCES lemma (id)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE,
    rank DOUBLE PRECISION NOT NULL
);

--changeset Stepnova Varvara:5
CREATE INDEX page_path_index ON page (path);