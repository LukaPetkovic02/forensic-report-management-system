package com.example.backend.service;

import com.example.backend.configuration.PasswordHasher;
import com.example.backend.model.LoginDetailsDTO;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public boolean login(LoginDetailsDTO loginDetailsDTO) {
        User user = userRepository.findByUsername(loginDetailsDTO.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return PasswordHasher.verifyPassword(loginDetailsDTO.getPassword(), user.getPassword());
    }
}
