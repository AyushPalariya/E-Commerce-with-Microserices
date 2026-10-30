package com.example.ApiGateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
@Component
public class JwtService {
    private String key;
    private final SecretKey secretKey;
    public JwtService(@Value("${security.jwt.key}") String key){
        this.key=key;
        secretKey= Keys.hmacShaKeyFor(this.key.getBytes(StandardCharsets.UTF_8));
    }

    public Jws<Claims> claims(String token){
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
    }
    public boolean isAccessToken(String token){
        Claims body = claims(token).getBody();
        return "access".equals(body.get("type"));
    }
    public String getUserEmail(String rfToken) {
        return claims(rfToken).getBody().getSubject();
    }
    public String getRole(String token) {
        return claims(token).getBody().get("role").toString();
    }
    public Integer getUserId(String token) {
        Integer id = (Integer) claims(token).getBody().get("id");
        System.out.println(id.getClass().getName());
        return id;
    }
}

