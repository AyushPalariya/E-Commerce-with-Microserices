package com.example.AuthService.Repository;

import com.example.AuthService.DTO.UserDataSend;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "USERSERVICE")
public interface FeignClientInterface {
    @PostMapping("/user/profile")
    ResponseEntity<String> sendData(@RequestBody UserDataSend profile);
}
