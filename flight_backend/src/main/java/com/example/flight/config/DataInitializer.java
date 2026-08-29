package com.example.flight.config;

import com.example.flight.entity.User;
import com.example.flight.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String adminEmail = "30136_spoorthi@katalystindia.org";

            User admin = userRepository.findByEmail(adminEmail)
                .orElseGet(User::new);


                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFirstName("Spoorthi");
                admin.setLastName("Admin");
                admin.setRole("ADMIN");
                admin.setEmailVerified(true);

                userRepository.save(admin);

                System.out.println("=================================");
                System.out.println("SkyRoute Admin account created");
                System.out.println("Email: "+adminEmail);
                System.out.println("Password: Admin@123");
                System.out.println("=================================");
        };
    }
}