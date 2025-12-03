package com.sitionix.athssox.util.database;

import com.sitionix.athssox.entity.UserEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.HashMap;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UsersTable {

    private final EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserEntity create(final UserEntity userEntity) {
        final GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();

        String sql = "INSERT INTO USERS " +
                "(USER_NAME, PASSWORD) " +
                "VALUES " +
                "(:username, :password)";

        this.jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("username", userEntity.getEmail())
                        .addValue("password", userEntity.getPasswordHash()),
                generatedKeyHolder,
                new String[] {"id"});

        userEntity.setId(generatedKeyHolder.getKey().longValue());
        return userEntity;
    }

    public List<UserEntity> findAll() {
        return this.entityManager.createQuery("FROM UserEntity user", UserEntity.class).getResultList();
    }

    public void deleteAll() {
        this.jdbcTemplate.update("DELETE FROM USERS", new HashMap<>());
    }
}
