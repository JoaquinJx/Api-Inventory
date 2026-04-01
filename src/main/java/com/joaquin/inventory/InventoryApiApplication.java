package com.joaquin.inventory;

import com.joaquin.inventory.entity.User;
import com.joaquin.inventory.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class InventoryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApiApplication.class, args);
    }

    // Crea usuario admin por defecto si no existe al arrancar
    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(User.builder()
                        .username("admin")
                        .email("admin@inventory.com")
                        .password(passwordEncoder.encode("Admin1234!"))
                        .role(User.Role.ADMIN)
                        .build());
            }
        };
    }
}
