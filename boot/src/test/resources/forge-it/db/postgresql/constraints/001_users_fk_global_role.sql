ALTER TABLE users
    ADD CONSTRAINT fk_users_global_role
        FOREIGN KEY (role_id) REFERENCES global_roles (id);
