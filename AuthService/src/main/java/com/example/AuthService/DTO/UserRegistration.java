package com.example.AuthService.DTO;

import com.example.AuthService.Entities.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserRegistration {
    private Long id;
    private String city;
    private String address;
    private String name;
    private String email;
    private String password;
    private String phone_no;
    private Boolean enable;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
