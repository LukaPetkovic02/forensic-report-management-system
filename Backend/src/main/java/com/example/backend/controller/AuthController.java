package com.example.backend.controller;

import com.example.backend.model.LoginDetailsDTO;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.TokenUtil;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenUtil tokenUtil;

    @GetMapping("/api/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authentication.getName());
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDetailsDTO loginDetailsDTO) {
        User user = userRepository.findByUsername(loginDetailsDTO.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (!userService.login(loginDetailsDTO))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        String jwt = tokenUtil.generateToken(user);
        return ResponseEntity.ok(jwt);
    }
}
