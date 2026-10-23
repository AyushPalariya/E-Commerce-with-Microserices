package com.example.UserService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name="user_profile")
@Data
@NoArgsConstructor
public class UserProfile implements Serializable {
    @Id
    private Long id;
    private String name;
    @Column(unique = true)
    private String email;
    private String phone_no;
    private String country;
    private String state;
    private String city;
    private String img;
    private String address;

    public UserProfile(Long id, String name, String email, String phone_no) {
        this.id=id;
        this.name = name;
        this.email = email;
        this.phone_no = phone_no;
    }
}
