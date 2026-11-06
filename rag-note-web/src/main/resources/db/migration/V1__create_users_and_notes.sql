-- 迁移角色需安装 vector 扩展；日常运行角色不应具备扩展管理权限。
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username varchar(32) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    role varchar(16) NOT NULL CHECK (role IN ('USER','ADMIN')),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE invitation (
    id uuid PRIMARY KEY,
    token_hash char(64) NOT NULL UNIQUE,
    created_by bigint NOT NULL REFERENCES app_user(id),
    expires_at timestamptz NOT NULL,
    used_by bigint REFERENCES app_user(id),
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((used_by IS NULL) = (used_at IS NULL))
);
CREATE TABLE note (
    id uuid PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES app_user(id),
    title varchar(200) NOT NULL,
    original_filename varchar(255) NOT NULL,
    byte_size integer NOT NULL CHECK (byte_size > 0 AND byte_size <= 2097152),
    content_sha256 char(64) NOT NULL,
    raw_markdown text NOT NULL,
    plain_text text NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'READY' CHECK (status = 'READY'),
    chunk_count integer NOT NULL CHECK (chunk_count BETWEEN 1 AND 128),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (owner_id, content_sha256),
    UNIQUE (id, owner_id)
);
CREATE INDEX idx_note_owner_created ON note(owner_id, created_at DESC, id DESC);
CREATE TABLE note_chunk (
    id uuid PRIMARY KEY,
    note_id uuid NOT NULL,
    owner_id bigint NOT NULL,
    chunk_index integer NOT NULL CHECK (chunk_index >= 0),
    heading_path text NOT NULL,
    start_line integer NOT NULL CHECK (start_line > 0),
    end_line integer NOT NULL CHECK (end_line >= start_line),
    content text NOT NULL,
    token_count integer NOT NULL CHECK (token_count > 0 AND token_count <= 600),
    embedding_model varchar(128) NOT NULL,
    embedding vector(1024) NOT NULL,
    FOREIGN KEY (note_id, owner_id) REFERENCES note(id, owner_id) ON DELETE CASCADE,
    UNIQUE(note_id, chunk_index)
);
CREATE INDEX idx_chunk_owner_note ON note_chunk(owner_id, note_id);
