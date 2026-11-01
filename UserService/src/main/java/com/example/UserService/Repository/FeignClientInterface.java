package com.example.UserService.Repository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.Delegate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AUTHSERVICE")
public interface FeignClientInterface {
    @DeleteMapping("/auth/delete/{email}")
    ResponseEntity<String> deleteUser(@PathVariable String email);
}
