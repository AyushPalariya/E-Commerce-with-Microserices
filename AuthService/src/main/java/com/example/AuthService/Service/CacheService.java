package com.example.AuthService.Service;

import com.example.AuthService.Entities.AuthUserEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CacheService {

    @CachePut(value = "rft",key = "#email")
    public String putJit(String email,String jti) {
        System.out.println("jti insert into cache.");
        return jti;
    }
    @Cacheable(value = "rft",key = "#email")
    public String getJti(String email) {
        return null;
    }

    @CacheEvict(value="rft",key="#email")
    public void removeJti(String email){
        System.out.println("cacheEvict for jti working..");
    }

    @CachePut(value = "users",key = "#user.username")
    public UserDetails putUser(UserDetails user) {
        System.out.println("user insert into cache.");
        return user;
    }
    @Cacheable(value = "users",key = "#email")
    public UserDetails getUser(String email) {
        return null;
    }

    @CacheEvict(value = "users",key = "#email")
    public void removeUser(String email){
        System.out.println("cacheEvict for user working..");
    }

    @CachePut(value = "otp",key = "#email")
    public String  generateOtp(String email) {
        Random random = new Random();
        int otp = random.nextInt(100000, 1000000);
        return String.valueOf(otp);
    }
    @Cacheable(value = "otp",key = "#email")
    public String getOtp(String email) {
        return null;
    }
    @CacheEvict(value = "otp",key = "#email")
    public void removeOtp(String email) {
        System.out.println("otp deleted from cache..");
    }
}
