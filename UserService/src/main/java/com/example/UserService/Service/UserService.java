package com.example.UserService.Service;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.GlobalExcep.BadRequestException;
import com.example.UserService.GlobalExcep.ConflictException;
import com.example.UserService.GlobalExcep.NotFoundException;
import com.example.UserService.Repository.FeignClientInterface;
import com.example.UserService.Repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;


@Service
@Data
public class UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired (required = true)
    private FeignClientInterface feignClientInterface;
    @Autowired
    private CacheManager cacheManager;


    @Transactional
    public String  createProfile(ProfileRequest profile) {
        if (profile == null) throw new BadRequestException("Profile details are required!");
        if(profile.getId()==null) throw new BadRequestException("Id is required!");
        if(profile.getId() <= 0) throw new BadRequestException("Id must be positive!");
        if(profile.getName()==null||profile.getName().isBlank()) throw new BadRequestException("Name is required!");
        if(profile.getEmail()==null||profile.getEmail().isBlank()) throw new BadRequestException("email is required!");
        if(profile.getPhone_no()==null||profile.getPhone_no().isBlank()) throw new BadRequestException("phone_no is required!");
        if(userRepo.existsById(profile.getId())) throw new ConflictException("Profile already exists for user id: " + profile.getId());
        if(userRepo.existsByEmail(profile.getEmail())) throw new ConflictException("Profile already exists for email: " + profile.getEmail());
        UserProfile userProfile=new UserProfile(profile.getId(), profile.getName(),profile.getEmail(),profile.getPhone_no());
        System.out.println("userId"+userProfile.getId());
        userRepo.save(userProfile);
        return "Profile Created!";
    }
    @CacheEvict(value = "profile",key = "#email")
    public UserProfile updateProfile(String email,Map<String , Object> fields) {
        UserProfile userProfile = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("User Profile not found!"));
        for(String key:fields.keySet()){
            if(key.equals("address")) userProfile.setAddress((String)fields.get(key));
            if(key.equals("img")) userProfile.setImg((String) fields.get(key));
            if(key.equals("state")) userProfile.setState((String)fields.get(key));
            if(key.equals("city")) userProfile.setCity((String) fields.get(key));
            if(key.equals("country")) userProfile.setCountry((String)fields.get(key));
        }
        userProfile=userRepo.save(userProfile);
        System.out.println("Profile update!");
        return userProfile;
    }

    @Cacheable(value = "profile",key="#email")
    public UserProfile getProfileByEmail(String email) {
        if (email == null || email.isBlank()) throw new BadRequestException("Email is required!");
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User profile not found for email: " + email));
    }

    public List<UserProfile> getAllUserProfile(){
        return userRepo.findAll();
    }
    @Transactional
    @CacheEvict(value = "profile",key="#email")
    public ResponseEntity<String> deleteUser(String email){
        if (email == null || email.isBlank()) throw new BadRequestException("Email is required!");
        if(!userRepo.existsByEmail(email)) throw new NotFoundException("User profile not found for email: " + email);
        ResponseEntity<String> s=webClientMethod(email);
        System.out.println(s);
        userRepo.deleteByEmail(email);
        System.out.println("User Profile is deleted from UserService with "+email);
        return s;
    }

    private ResponseEntity<String> webClientMethod(String email) {
        System.out.println(email);
        return feignClientInterface.deleteUser(email);
    }

}
