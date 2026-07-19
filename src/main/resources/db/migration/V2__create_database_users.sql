CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    enabled BIT(1) NOT NULL,
    locked BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
