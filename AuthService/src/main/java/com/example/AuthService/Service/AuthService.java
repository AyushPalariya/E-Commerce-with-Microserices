package com.example.AuthService.Service;

import com.example.AuthService.DTO.*;
import com.example.AuthService.Entities.AuthUserEntity;
import com.example.AuthService.Entities.Role;
import com.example.AuthService.GlobalException.AuthenticationExceptionClass;
import com.example.AuthService.GlobalException.BadRequestExceptionClass;
import com.example.AuthService.GlobalException.ConflictException;
import com.example.AuthService.GlobalException.NotFoundClass;
import com.example.AuthService.JwtUtils.JwtService;
import com.example.AuthService.Repository.AuthRepo;
import com.example.AuthService.Repository.FeignClientInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
    private final FeignClientInterface feignClientInterface;
    @Autowired
    private KafkaTemplate<String, Object> template;
    @Value("${kafka.topic.name}")
    private String topicName;
    @Value("${kafka.topic.reset}")
    private String resetTopic;

    @Transactional
    public UserRegistration register(UserRegistration userRegistration) {
        if (userRegistration == null) throw new BadRequestExceptionClass("Registration details are required!");
        if (userRegistration.getEmail() == null || userRegistration.getEmail().isBlank())
            throw new BadRequestExceptionClass("Please enter mail id!");
        if (userRegistration.getName() == null || userRegistration.getName().isBlank())
            throw new BadRequestExceptionClass("Please enter name!");
        if (userRegistration.getPhone_no() == null || userRegistration.getPhone_no().isBlank())
            throw new BadRequestExceptionClass("Please enter phone_no!");
        if (userRegistration.getPassword() == null || userRegistration.getPassword().isBlank())
            throw new BadRequestExceptionClass("Please enter password!");
        if (authRepo.existsByEmail(userRegistration.getEmail())) throw new ConflictException("Email already exist.");
        AuthUserEntity u = new AuthUserEntity();
        u.setEmail(userRegistration.getEmail());
        u.setName(userRegistration.getName());
        u.setPhone_no(userRegistration.getPhone_no());
        u.setEnable(true);
        u.setPassword(passwordEncoder.encode(userRegistration.getPassword()));
        u.setRole(Role.ROLE_USER);
        u = authRepo.save(u);

        ResponseEntity<String> s = sendData(u);
        if (s == null || !s.getStatusCode().is2xxSuccessful())
            throw new IllegalStateException("Profile service did not create the user profile");
        template.send(
                topicName,
                String.valueOf(u.getId()),
                new RegistrationEmailEvent(
                        u.getId(),
                        u.getName(),
                        u.getEmail(),
                        u.getPhone_no(),
                        u.getRole().toString()
                )
        );
        return new UserRegistration(u.getId(), u.getName(), u.getPassword(), u.getEmail(), u.getPhone_no(), u.getEnable(), u.getRole(), u.getCreatedAt(), u.getUpdatedAt());
    }

    private ResponseEntity<String> sendData(AuthUserEntity user) {
        UserDataSend send = new UserDataSend(user.getId(), user.getName(), user.getEmail(), user.getPhone_no());
        System.out.println(send);
        String json = new ObjectMapper().writeValueAsString(send);
        System.out.println("ObjectMapper use\n" + json);
        return feignClientInterface.sendData(send);
    }

    public String refreshTokenJti() {
        return UUID.randomUUID().toString();
    }

    public LoginResponse login(UserLoginRequest userLogin, HttpServletResponse response) {
        if (userLogin == null) throw new BadRequestExceptionClass("Login details are required!");
        if (userLogin.getEmail() == null || userLogin.getEmail().isBlank())
            throw new BadRequestExceptionClass("Please enter mail id!");
        if (userLogin.getPassword() == null || userLogin.getPassword().isBlank())
            throw new BadRequestExceptionClass("Please enter password!");
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword()));
        System.out.println("authentication successful");
        UserDetails details = (UserDetails) auth.getPrincipal();
        String email=details.getUsername();
        AuthUserEntity data = cacheService.getAuthUser(email);
        String accessToken = jwtService.generateToken(details,data.getId());
        String jti = refreshTokenJti();
        String refreshToken = jwtService.refreshToken(details, jti,data.getId());

        cacheService.putJit(email, jti);
        cacheService.putUser(details);

        cookieService.attachToken(refreshToken, jwtService.getRftAccessibleTime(), response);
        String role = jwtService.getRole(details);
        return new LoginResponse(data.getId(),data.getName() ,accessToken, refreshToken, details.getUsername(), Role.valueOf(role));
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        String token = readToken(refreshToken, request);
        if (token == null) throw new AuthenticationExceptionClass("Refresh token missing!");
        if (!jwtService.isRefreshToken(token)) {
            throw new AuthenticationExceptionClass("Invalid refresh token type!");
        }
        String email = jwtService.getUserEmail(token);
        if (email == null || email.isBlank()) throw new AuthenticationExceptionClass("Email missing!");
        System.out.println("refresh token email: " + email);

        UserDetails user = cacheService.getUser(email);
        if (user == null) throw new AuthenticationExceptionClass("user not present in cache!");
        System.out.println("usercahce: " + user.getUsername());

        if (!email.equals(user.getUsername())) throw new AuthenticationExceptionClass("Email mismatched!");
        String cacheJti = cacheService.getJti(email);
        if (cacheJti == null) throw new AuthenticationExceptionClass("cache Jti is missing");
        System.out.println("cachejti: " + cacheJti);
        if (!cacheJti.equals(jwtService.getJti(token))) throw new AuthenticationExceptionClass("Jti mismatched!");
        AuthUserEntity data = cacheService.getAuthUser(email);
        String accessToken = jwtService.generateToken(user, data.getId());
        String jti = refreshTokenJti();
        String refreshToken1 = jwtService.refreshToken(user, jti, data.getId());
        cacheService.putJit(email, jti);
        cacheService.putUser(user);
        cookieService.attachToken(refreshToken1, jwtService.getRftAccessibleTime(), response);
        String role = jwtService.getRole(user);
        return new LoginResponse(data.getId(),data.getName(),accessToken, refreshToken1, user.getUsername(), Role.valueOf(role));
    }

    private String readToken(String refreshToken, HttpServletRequest request) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            return refreshToken;
        }
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(cookieService.getRefreshTokenName()))
                    .map(Cookie::getValue).filter(v -> !v.isBlank()).findFirst().orElse(null);

        }
        return null;
    }

    @Transactional
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String s = readToken(null, req);
        if (s == null) throw new AuthenticationExceptionClass("refresh token missing from logout!");
        String email = jwtService.getUserEmail(s);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        cacheService.removeAuthUser(email);
        cookieService.clear(res);
        cookieService.clearHeader(res);
        SecurityContextHolder.clearContext();
        System.out.println("logout success for user :" + email);
    }

    @Transactional
    public String delete(String email, HttpServletResponse res) {
        if (email == null || email.isBlank()) throw new BadRequestExceptionClass("Email is required!");
        if (!authRepo.existsByEmail(email)) throw new NotFoundClass("User not found for email: " + email);
        authRepo.deleteByEmail(email);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        cacheService.removeAuthUser(email);
        cookieService.clear(res);
        cookieService.clearHeader(res);
        return "User deleted form authService with email_id:" + email;
    }
    @Transactional
    public Map<String, String> forgot(Map<String, String> mp) {
        //"email":"..."
        if (mp == null) throw new BadRequestExceptionClass("mp not be null!");
        String email = mp.get("email");
        if (email == null || email.isBlank()) throw new BadRequestExceptionClass("Email is required!");
        AuthUserEntity authUserEntity = authRepo.findByEmail(email).orElseThrow(() -> new NotFoundClass("email not found!"));
        ForgotPassword forget = new ForgotPassword();
        forget.setEmail(email);
        forget.setName(authUserEntity.getName());
        String otp=cacheService.generateOtp(email);
        forget.setOtp(otp);

        template.send(resetTopic, email, forget);

        return Map.of("email", email,"datetime",String.valueOf(LocalDateTime.now().plusMinutes(5)));
    }

    @Transactional
    public Map<String, String> resetPassword(PasswordResetRequest request) {
        if (request == null) throw new BadRequestExceptionClass("Password reset details are required!");
        String email = request.getEmail();
        if (email == null || email.isBlank()) throw new BadRequestExceptionClass("Email invalid!");
        if (request.getNewPassword() == null || request.getNewPassword().isBlank())
            throw new BadRequestExceptionClass("Password required!");
        if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank())
            throw new BadRequestExceptionClass("Confirm password required!");
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new BadRequestExceptionClass("Password does not match the confirmation password!");
        if (!authRepo.existsByEmail(email)) throw new NotFoundClass("User not found for email: " + email);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        int v=authRepo.savePasswordByEmail(email,passwordEncoder.encode(request.getConfirmPassword()),LocalDateTime.now());
        if(v==0) throw new NotFoundClass("Password not update! "+email);
        return Map.of("SUCCESS","Password changed");
    }

    public Map<String, String> validateOtp(OtpToken otpToken) {
        if (otpToken == null) throw new BadRequestExceptionClass("OTP details are required!");
        if (otpToken.getEmail() == null || otpToken.getEmail().isBlank())
            throw new BadRequestExceptionClass("Email is required!");
        if (otpToken.getOtp() == null || otpToken.getOtp().isBlank())
            throw new BadRequestExceptionClass("OTP is required!");
        if (otpToken.getDatetime() == null || otpToken.getDatetime().isBlank())
            throw new BadRequestExceptionClass("OTP expiration time is required!");
        LocalDateTime afterTime;
        try {
            afterTime = LocalDateTime.parse(otpToken.getDatetime());
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestExceptionClass("OTP expiration time is invalid!");
        }
        if(LocalDateTime.now().isAfter(afterTime)) {
            cacheService.removeOtp(otpToken.getEmail());
            throw new AuthenticationExceptionClass("Otp expired!");
        }
        String otp = cacheService.getOtp(otpToken.getEmail());
        if(otp==null||!otp.equals(otpToken.getOtp())) {
            throw new AuthenticationExceptionClass("Otp incorrect!");
        }
        cacheService.removeOtp(otpToken.getEmail());
        return Map.of("SUCCESS","OTP Correct");
    }


}
