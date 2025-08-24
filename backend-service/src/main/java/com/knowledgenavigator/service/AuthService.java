package com.knowledgenavigator.service;

import com.knowledgenavigator.dto.LoginRequest;
import com.knowledgenavigator.dto.RegisterRequest;
import com.knowledgenavigator.model.Role;
import com.knowledgenavigator.model.User;
import com.knowledgenavigator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setGroupIds(new ArrayList<>());
        user.setCreatedDate(LocalDateTime.now());
        user.setActive(true);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getGroupIds());

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name()
            )
        );
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getGroupIds());

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "groups", user.getGroupIds()
            )
        );
    }
}