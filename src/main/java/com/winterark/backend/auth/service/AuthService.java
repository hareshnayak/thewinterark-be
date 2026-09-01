package com.winterark.backend.auth.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.auth.payload.AuthRequestDTO;
import com.winterark.backend.auth.payload.AuthResponseDTO;
import com.winterark.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    private static final java.util.regex.Pattern EMAIL_REGEX = 
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Transactional
    public AuthResponseDTO register(AuthRequestDTO request) {
        if (request.getEmail() == null || !EMAIL_REGEX.matcher(request.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Please provide a valid email address");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (userRepository.existsByUsername(request.getUsername().trim()) || userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Username or Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(jwt)
                .build();
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        // Here we expect the login request to use username or email
        User user = null;
        if(request.getUsername() != null && !request.getUsername().isEmpty()) {
            user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with username"));
        }else if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email"));
        } else {
            throw new IllegalArgumentException("Username or Email must be provided");
        }
        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(jwt)
                .build();
    }
}
