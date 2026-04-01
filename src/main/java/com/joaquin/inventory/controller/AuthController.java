package com.joaquin.inventory.controller;

import com.joaquin.inventory.dto.auth.LoginRequest;
import com.joaquin.inventory.dto.auth.LoginResponse;
import com.joaquin.inventory.dto.auth.RefreshTokenRequest;
import com.joaquin.inventory.dto.auth.RegisterRequest;
import com.joaquin.inventory.dto.auth.UserProfileResponse;
import com.joaquin.inventory.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String rateLimitKey = buildRateLimitKey(request.getUsername(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(authService.login(request, rateLimitKey));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    private String buildRateLimitKey(String username, String ip) {
        return (ip == null ? "unknown-ip" : ip) + ":" + username.toLowerCase();
    }
}
