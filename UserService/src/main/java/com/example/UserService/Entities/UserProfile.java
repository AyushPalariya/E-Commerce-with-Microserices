package com.example.UserService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name="user_profile")
@Data

public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public UserProfile( String name, String email, String phone_no) {
        this.name = name;
        this.email = email;
        this.phone_no = phone_no;
    }
}
