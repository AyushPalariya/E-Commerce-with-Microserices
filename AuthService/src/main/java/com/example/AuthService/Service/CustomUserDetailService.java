package com.example.AuthService.Service;

import com.example.AuthService.Entities.AuthUserEntity;
import com.example.AuthService.Repository.AuthRepo;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final AuthRepo authRepo;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            AuthUserEntity user=authRepo.findByEmail(email).orElseThrow(()->new BadCredentialsException("User not found!Please enter correct email address"));
            System.out.println(user.getName()+"-"+user.getEmail());
            SimpleGrantedAuthority authority=new SimpleGrantedAuthority(user.getRole().toString());
            return new User(user.getEmail(),user.getPassword(),user.getEnable(),
                    true,true,true, List.of(authority)
                    );

    }
}
