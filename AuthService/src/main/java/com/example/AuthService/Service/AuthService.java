package com.example.AuthService.Service;

import com.example.AuthService.DTO.LoginResponse;
import com.example.AuthService.DTO.UserDataSend;
import com.example.AuthService.DTO.UserLoginRequest;
import com.example.AuthService.DTO.UserRegistration;
import com.example.AuthService.Entities.AuthUserEntity;
import com.example.AuthService.Entities.Role;
import com.example.AuthService.GlobalException.AuthenticationExceptionClass;
import com.example.AuthService.GlobalException.ConflictException;
import com.example.AuthService.JwtUtils.JwtService;
import com.example.AuthService.Repository.AuthRepo;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final CacheService cacheService;
    private final AuthRepo authRepo;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RestClient restclient;
    @Transactional
    public UserRegistration register(UserRegistration userRegistration) {
        if(userRegistration.getEmail()==null||userRegistration.getEmail().isBlank()) throw new BadCredentialsException("Please enter mail id!");
        if(userRegistration.getName()==null||userRegistration.getName().isBlank()) throw new BadCredentialsException("Please enter name!");
        if(userRegistration.getPhone_no()==null||userRegistration.getPhone_no().isBlank()) throw new BadCredentialsException("Please enter phone_no!");
        if(authRepo.existsByEmail(userRegistration.getEmail())) throw new ConflictException("Email already exist.");
        AuthUserEntity u=new AuthUserEntity();
        u.setEmail(userRegistration.getEmail());
        u.setName(userRegistration.getName());
        u.setPhone_no(userRegistration.getPhone_no());
        u.setEnable(true);
        u.setPassword(passwordEncoder.encode("dummy123"));
        u.setRole(Role.ROLE_USER);
        u=authRepo.save(u);
        System.out.println(u);
        //restClient
        String s=sendData(u);
        if(s==null) throw new RuntimeException("Profile is not created for user!");
        //
        return new UserRegistration(u.getId(),u.getName(),u.getEmail(),u.getPhone_no(),u.getEnable(),u.getRole(),u.getCreatedAt(),u.getUpdatedAt());
    }

    private String sendData(AuthUserEntity user) {
        UserDataSend send=new UserDataSend(user.getName(),user.getEmail(),user.getPhone_no());
        System.out.println(send);
        String json=new ObjectMapper().writeValueAsString(send);
        System.out.println("ObjectMapper use\n"+json);
        return restclient.post()
                .uri("http://localhost:8083/user/profile")
                .body(send)
                .retrieve()
                .body(String.class);
    }

    public String refreshTokenJti(){
        return UUID.randomUUID().toString();
    }
    public LoginResponse login(UserLoginRequest userLogin,HttpServletResponse response) {
        if (userLogin.getEmail() == null || userLogin.getEmail().isBlank())
            throw new BadCredentialsException("Please enter mail id!");
        if (userLogin.getPassword() == null || userLogin.getPassword().isBlank()) throw new BadCredentialsException("Please enter password!");
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword()));
            System.out.println("authentication successful");
            UserDetails details = (UserDetails) auth.getPrincipal();
            String accessToken = jwtService.generateToken(details);
            String jti=refreshTokenJti();
            String refreshToken = jwtService.refreshToken(details,jti);
            cacheService.putJit(details.getUsername(), jti);
            cacheService.putUser(details);
            cookieService.attachToken(refreshToken, jwtService.getRftAccessibleTime(), response);
            String role = jwtService.getRole(details);
            return new LoginResponse(accessToken, refreshToken, details.getUsername(), Role.valueOf(role));
    }

    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request,HttpServletResponse response) {
        String token=readToken(refreshToken,request);
        if(token==null) throw new AuthenticationExceptionClass("Refresh token missing!");
        if(!jwtService.isRefreshToken(token)){
            throw new AuthenticationExceptionClass("Invalid refresh token type!");
        }
        String email=jwtService.getUserEmail(token);
        if (email == null || email.isBlank()) throw new AuthenticationExceptionClass("Email missing!");
        System.out.println("token email: "+email);

        UserDetails user=cacheService.getUser(email);
        if(user==null) throw new AuthenticationExceptionClass("user not present in cache!");
        System.out.println("usercahce: "+user.getUsername());

        if(!email.equals(user.getUsername())) throw new AuthenticationExceptionClass("Email mismatched!");
        String cacheJti=cacheService.getJti(email);
        if(cacheJti == null) throw new AuthenticationExceptionClass("cache Jti is missing");
        System.out.println("cachejti: "+cacheJti);
        if ( !cacheJti.equals(jwtService.getJti(token))) throw new AuthenticationExceptionClass("Jti mismatched!");
        String accessToken=jwtService.generateToken(user);
        String jti=refreshTokenJti();
        String refreshToken1= jwtService.refreshToken(user,jti);
        cacheService.putJit(email,jti);
        cacheService.putUser(user);
        cookieService.attachToken(refreshToken1,jwtService.getRftAccessibleTime(),response);
        String role = user.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(()->new RuntimeException("User has no role"));
        return new LoginResponse(accessToken,refreshToken1, user.getUsername(),Role.valueOf(role));

    }

    private String readToken(String refreshToken, HttpServletRequest request) {
        if(refreshToken!=null&&!refreshToken.isBlank()){
            return refreshToken;
        }
        if(request.getCookies()!=null){
            return Arrays.stream(request.getCookies()).filter(c->c.getName().equals(cookieService.getRefreshTokenName()))
                    .map(Cookie::getValue).filter(v->!v.isBlank()).findFirst().orElse(null);

        }
        return null;
    }

    public void logout(HttpServletRequest req,HttpServletResponse res) {
        String s = readToken(null, req);
        if(s==null) throw new BadCredentialsException("refresh token missing from logout!");
        String email=jwtService.getUserEmail(s);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        cookieService.clear(res);
        cookieService.clearHeader(res);
        SecurityContextHolder.clearContext();
    }
}
