package com.example.UserService.Service;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.GlobalExcep.BadCredentialClass;
import com.example.UserService.GlobalExcep.ConflictException;
import com.example.UserService.Repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Data
public class UserService {
    @Autowired
    private UserRepo userRepo;
    @Transactional
    public String  createProfile(ProfileRequest profile) {
        if(profile.getName()==null||profile.getName().isBlank()) throw new BadCredentialClass("Name is required!");
        if(profile.getEmail()==null||profile.getEmail().isBlank()) throw new BadCredentialClass("email is required!");
        if(profile.getPhone_no()==null||profile.getPhone_no().isBlank()) throw new BadCredentialClass("phone_no is required!");
        UserProfile userProfile=new UserProfile(profile.getPhone_no(),profile.getEmail(),profile.getName());
        userRepo.save(userProfile);
        return "Profile Created!";
    }
}
