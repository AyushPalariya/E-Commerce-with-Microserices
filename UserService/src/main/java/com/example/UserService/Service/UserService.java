package com.example.UserService.Service;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.GlobalExcep.BadCredentialClass;
import com.example.UserService.GlobalExcep.ConflictException;
import com.example.UserService.Repository.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.Data;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;


@Service
@Data
public class UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private WebClient webClient;
    @Transactional
    public String  createProfile(ProfileRequest profile) {
        if(profile.getId()==null) throw new BadCredentialClass("Id is required!");
        if(profile.getName()==null||profile.getName().isBlank()) throw new BadCredentialClass("Name is required!");
        if(profile.getEmail()==null||profile.getEmail().isBlank()) throw new BadCredentialClass("email is required!");
        if(profile.getPhone_no()==null||profile.getPhone_no().isBlank()) throw new BadCredentialClass("phone_no is required!");
        UserProfile userProfile=new UserProfile(profile.getId(), profile.getName(),profile.getEmail(),profile.getPhone_no());
        System.out.println("userId"+userProfile.getId());
        userRepo.save(userProfile);
        return "Profile Created!";
    }

    public UserProfile updateProfile(Long id,Map<String , Object> fields) {
        UserProfile userProfile = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User Profile not found!"));
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
    @Cacheable(value = "profile",key="#id")
    public UserProfile getProfile(Long id){
        return userRepo.findById(id).orElseThrow(() -> new NotFoundException("User Profile not found!"));
    }

    public List<UserProfile> getAllUserProfile(){
        return userRepo.findAll();
    }
    @CacheEvict(value = "profile",key = "#id")
    public void deleteUser(Long id){
        UserProfile userProfile = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User Profile not found!"));
        String s=webClientMethod(userProfile.getEmail());
        System.out.println(s);
        userRepo.deleteById(id);
        System.out.println("User Profile is deleted from UserService with "+id);
    }

    private String webClientMethod(String email) {
        System.out.println(email);
        return webClient.delete()
                .uri("http://localhost:8082/auth/delete/{email}",email)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
