package com.example.AuthService.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Data
public class CookieService {
    private final String refreshTokenName;
    private final Boolean cookieSecure;
    private final Boolean cookieHttpOnly;
    private final String cookieDomain;
    private final String cookieSameSite;

    public CookieService(@Value("${security.jwt.cookie.refreshTokenName}") String refreshTokenName,
                         @Value("${security.jwt.cookie.secure}") Boolean cookieSecure,
                         @Value("${security.jwt.cookie.httpOnly}") Boolean cookieHttpOnly,
                         @Value("${security.jwt.cookie.domain}") String cookieDomain,
                         @Value("${security.jwt.cookie.sameSite}") String cookieSameSite) {
        this.refreshTokenName = refreshTokenName;
        this.cookieSecure = cookieSecure;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }

    public void attachToken(String rft, int validUpto, HttpServletResponse response){
        ResponseCookie build = ResponseCookie.from(refreshTokenName, rft).domain(cookieDomain).httpOnly(cookieHttpOnly).secure(cookieSecure)
                .sameSite(cookieSameSite).path("/").maxAge(Duration.ofSeconds(validUpto)).build();
        response.addHeader(HttpHeaders.SET_COOKIE,build.toString());
    }
    public void clear(HttpServletResponse response){
        ResponseCookie build = ResponseCookie.from(refreshTokenName, "").domain(cookieDomain).httpOnly(cookieHttpOnly).secure(cookieSecure)
                .sameSite(cookieSameSite).path("/").maxAge(Duration.ofSeconds(0)).build();
        response.addHeader(HttpHeaders.SET_COOKIE,build.toString());
    }
    public void clearHeader(HttpServletResponse res){
        res.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
        res.setHeader(HttpHeaders.PRAGMA,"no-cache");
    }

}
