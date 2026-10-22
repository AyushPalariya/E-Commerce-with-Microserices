package com.example.UserService.DTO;

public record ProfileResponse(String name,
                              String email,
                              String phone_no,
                              String country,
                              String state,
                              String city,
                              String img,
                              String address) {
}
