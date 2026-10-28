package com.example.UserService.Repository;

import com.example.UserService.Entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<UserProfile,Long> {

    Optional<UserProfile> findByEmail(String email);

    Boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
