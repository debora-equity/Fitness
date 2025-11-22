package com.softgenia.playlist;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.entity.Role;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.RolesRepository;
import com.softgenia.playlist.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RolesRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Initialize the NEW Roles
            if (roleRepository.count() == 0) {
                System.out.println("--- Seeding Roles ---");
                roleRepository.save(new Role(Roles.ROLE_ADMIN));
                roleRepository.save(new Role(Roles.ROLE_USER));
                roleRepository.save(new Role(Roles.ROLE_CONTENT_CREATOR));
            }

            // 2. Initialize an ADMIN User (instead of SUPER_ADMIN)
            if (!userRepository.existsByUsername("admin")) {
                // Fetch the ROLE_ADMIN entity you just saved
                Role adminRole = roleRepository.findByName(Roles.ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("adminpassword"));
                adminUser.setEmail("admin@app.com");
                adminUser.setName("Admin");
                adminUser.setSurname("User");
                adminUser.setRole(adminRole);

                userRepository.save(adminUser);
                System.out.println("--- Initial ADMIN user created. Credentials: admin/adminpassword ---");
            }
        };
    }
}
