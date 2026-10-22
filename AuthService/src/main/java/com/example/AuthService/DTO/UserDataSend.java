package com.example.AuthService.DTO;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public class UserDataSend implements Serializable {
    private String name;
    private String email;
    private String phone_no;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone_no() {
        return phone_no;
    }

    public void setPhone_no(String phone_no) {
        this.phone_no = phone_no;
    }

    @Override
    public String toString() {
        return "UserDataSend{" +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone_no='" + phone_no + '\'' +
                '}';
    }
}
