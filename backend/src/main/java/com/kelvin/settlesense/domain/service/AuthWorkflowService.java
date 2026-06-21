package com.kelvin.settlesense.domain.service;

import com.kelvin.settlesense.domain.model.User;
import com.kelvin.settlesense.domain.repository.UserRepository;
import com.kelvin.settlesense.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Optional;

@Service
public class AuthWorkflowService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    AuthWorkflowService(JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateToken(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPasswordHash()))
            throw new BadCredentialsException("Credentials did not match!");

        HashMap<String, String> claims = new HashMap<>();
        claims.put("email", user.get().getEmail());
        claims.put("displayName", user.get().getDisplayName());

        return jwtService.generateTokenWithDisplayName(claims, user.get());
    }

}
