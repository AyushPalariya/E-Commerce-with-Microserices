package com.example.AuthService.Repository;

import com.example.AuthService.Entities.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepo extends JpaRepository<AuthUserEntity,Long> {
    Optional<AuthUserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);
}
