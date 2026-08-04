package com.equipment.loan.service;

import com.equipment.loan.dto.request.LoginRequest;
import com.equipment.loan.dto.request.RegisterRequest;
import com.equipment.loan.dto.response.AuthResponse;
import com.equipment.loan.dto.response.UserResponse;
import com.equipment.loan.entity.User;
import com.equipment.loan.exception.DuplicateEmailException;
import com.equipment.loan.repository.UserRepository;
import com.equipment.loan.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        // This throws BadCredentialsException if email/password don't match,
        // which GlobalExceptionHandler turns into a clean 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, UserResponse.from(user));
    }
}
