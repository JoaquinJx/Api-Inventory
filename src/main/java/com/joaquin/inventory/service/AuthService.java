package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.auth.LoginRequest;
import com.joaquin.inventory.dto.auth.LoginResponse;
import com.joaquin.inventory.dto.auth.RefreshTokenRequest;
import com.joaquin.inventory.dto.auth.RegisterRequest;
import com.joaquin.inventory.dto.auth.UserProfileResponse;
import com.joaquin.inventory.entity.User;
import com.joaquin.inventory.exception.InvalidCredentialsException;
import com.joaquin.inventory.exception.ResourceNotFoundException;
import com.joaquin.inventory.repository.UserRepository;
import com.joaquin.inventory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimitService loginRateLimitService;

    public LoginResponse login(LoginRequest request, String rateLimitKey) {
        loginRateLimitService.assertAllowed(rateLimitKey);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimitService.onFailure(rateLimitKey);
            throw new InvalidCredentialsException();
        }

        loginRateLimitService.onSuccess(rateLimitKey);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(token, refreshToken, user.getUsername(), user.getRole().name());
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' already exists");
        }

        User user = userRepository.save(User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(token, refreshToken, user.getUsername(), user.getRole().name());
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.consumeValidToken(request.getRefreshToken());
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(token, refreshToken, user.getUsername(), user.getRole().name());
    }

    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        userRepository.findByUsername(authentication.getName())
                .ifPresent(refreshTokenService::revokeAll);
    }

    public UserProfileResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResourceNotFoundException("Authenticated user", "username", "anonymous");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
