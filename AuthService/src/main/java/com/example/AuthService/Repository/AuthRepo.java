package com.example.AuthService.Repository;

import com.example.AuthService.Entities.AuthUserEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthRepo extends JpaRepository<AuthUserEntity,Long> {
    Optional<AuthUserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);

    void deleteByEmail(String email);

    @Modifying
    @Query("""
    UPDATE AuthUserEntity a
    SET a.password = :password,
        a.updatedAt = :updatedAt
    WHERE a.email = :email
    """)
    int savePasswordByEmail(@Param("email") String email,
                             @Param("password") String confirmPassword,
                             @Param("updatedAt") LocalDateTime updatedAt);
}
