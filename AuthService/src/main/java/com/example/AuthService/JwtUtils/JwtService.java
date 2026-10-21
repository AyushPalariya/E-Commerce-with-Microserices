package com.example.AuthService.JwtUtils;

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
@Data
public class JwtService {
    private String key;
    private final SecretKey secretKey;
    private final int rftAccessibleTime;

    public JwtService(@Value("${security.jwt.key}") String key,@Value("${security.jwt.rftAccessible}") int rftAccessibleTime){
        this.key=key;
        secretKey= Keys.hmacShaKeyFor(this.key.getBytes(StandardCharsets.UTF_8));
        this.rftAccessibleTime=rftAccessibleTime;
    }
    public String getRole(UserDetails details){
        return details.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(()->new RuntimeException("User has no role"));
    }

    public String generateToken(UserDetails details) {
        String role=getRole(details);
        return Jwts.builder()
                .setSubject(details.getUsername())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+(600*1000)))//10 min
                .addClaims(Map.of("type","access","role",role))
                .signWith(secretKey)
                .compact();
    }

    public String refreshToken(UserDetails details,String jti) {
        String role=getRole(details);
        return Jwts.builder()
                .setSubject(details.getUsername())
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+(rftAccessibleTime * 1000L))) //200min means 3h:20 min
                .addClaims(Map.of("type","refresh","role",role))
                .signWith(secretKey)
                .compact();
    }

    public Jws<Claims> claims(String token){
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
    }
    public boolean isRefreshToken(String token){
        Claims body = claims(token).getBody();
        return "refresh".equals(body.get("type"));
    }
    public String getJti(String rfToken){
        return claims(rfToken).getBody().getId();
    }
    public String getUserEmail(String rfToken) {
        return claims(rfToken).getBody().getSubject();
    }
    public Date getExpiredTime(String token){
        return claims(token).getBody().getExpiration();
    }


}
