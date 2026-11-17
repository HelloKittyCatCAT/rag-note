CREATE TABLE conversation (
    id uuid PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES app_user(id),
    title varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, owner_id)
);
CREATE INDEX idx_conversation_owner_updated ON conversation(owner_id, updated_at DESC, id);

-- 一行保存一对问答。answer_json 非空表示已完成，lease_token 非空表示正在处理。
CREATE TABLE conversation_turn (
    seq bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id uuid NOT NULL,
    owner_id bigint NOT NULL,
    client_message_id uuid NOT NULL,
    question text NOT NULL,
    request_hash char(64) NOT NULL,
    answer_json jsonb,
    lease_token uuid,
    lease_until timestamptz,
    FOREIGN KEY (conversation_id, owner_id) REFERENCES conversation(id, owner_id) ON DELETE CASCADE,
    UNIQUE (conversation_id, client_message_id),
    CHECK ((lease_token IS NULL) = (lease_until IS NULL)),
    CHECK (answer_json IS NULL OR lease_token IS NULL)
);
CREATE UNIQUE INDEX idx_conversation_one_active_turn
    ON conversation_turn(conversation_id) WHERE lease_token IS NOT NULL;
CREATE INDEX idx_conversation_history ON conversation_turn(owner_id, conversation_id, seq DESC)
    WHERE answer_json IS NOT NULL;
