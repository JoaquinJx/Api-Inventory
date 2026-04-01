package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.auth.LoginRequest;
import com.joaquin.inventory.dto.auth.RegisterRequest;
import com.joaquin.inventory.entity.User;
import com.joaquin.inventory.exception.InvalidCredentialsException;
import com.joaquin.inventory.repository.UserRepository;
import com.joaquin.inventory.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginRateLimitService loginRateLimitService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jane");
        request.setEmail("jane@example.com");
        request.setPassword("Password123");

        when(userRepository.existsByUsername("jane")).thenReturn(false);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(jwtUtil.generateToken("jane", "USER")).thenReturn("jwt-token");
        when(refreshTokenService.issue(any(User.class))).thenReturn("refresh-token");

        var response = authService.register(request);

        assertThat(response.getUsername()).isEqualTo("jane");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_throwsOnInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad-password");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(User.builder()
                .username("admin")
                .password("encoded")
                .role(User.Role.ADMIN)
                .build()));
        when(passwordEncoder.matches("bad-password", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1:admin"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }
}