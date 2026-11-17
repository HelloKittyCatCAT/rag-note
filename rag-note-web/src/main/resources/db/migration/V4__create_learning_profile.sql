CREATE TABLE learning_profile (
    owner_id bigint PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    enabled boolean NOT NULL DEFAULT false,
    learning_goal varchar(500),
    explanation_preference varchar(500)
);
