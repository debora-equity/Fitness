package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.user.AdminCreateUserRequest;
import com.softgenia.playlist.model.dto.user.LoginDto;
import com.softgenia.playlist.model.dto.user.RegisterDto;
import com.softgenia.playlist.model.entity.Role;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.RolesRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.security.JwtTokenProvider;
import com.softgenia.playlist.service.UserDetailsServiceImpl;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RolesRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.jwt-expiration-milliseconds}")
    private long jwtExpirationDate;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody LoginDto loginDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken
                            (loginDto.getUsername(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);
            String username = authentication.getName();

            User user = userRepository.findByUsername(username).orElseThrow(
                    () -> new RuntimeException("User not found after authentication"));

            String role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);


            response.put("token", token);
            response.put("message", "Login successful!");
            response.put("roles", role);
            response.put("name", user.getName());
            response.put("surname", user.getSurname());
            response.put("image", user.getProfileImage());
            response.put("expiresIn", jwtExpirationDate);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDto registerDto) {

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Username already taken! ");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "This email is already registered! ");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        User user = new User();
        user.setName(registerDto.getName());
        user.setUsername(registerDto.getUsername());
        user.setSurname(registerDto.getSurname());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        Role defaultRole = roleRepository.findByName(Roles.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Default ROLE_USER not found."));
        user.setRole(defaultRole);

        userRepository.save(user);

        // --- NEW: SEND WELCOME EMAIL ---
        // We run this in a separate thread or try-catch block so that
        // if the email fails, the registration itself doesn't fail.
        try {
            sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
            // We do NOT return an error to the user here, because the account WAS created.
        }
        // -------------------------------

        return new ResponseEntity<>("User registered successfully as USER!", HttpStatus.CREATED);
    }

    // --- HELPER METHOD TO SEND EMAIL ---
    private void sendWelcomeEmail(String toEmail, String username) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(new InternetAddress(fromEmail, fromName));
        helper.setTo(toEmail);
        helper.setSubject("Miresevini ne Vio App!");

        String emailContent = "Pershendetje " + username + ",\n\n" +
                "Faleminderit qe u regjistruat ne Vio App!\n\n" +
                "Behuni gati per te filluar transformimin tuaj.\n\n" +
                "Gjithe te mirat\n" +
                "Vio App Team\n\n" +
                "Per t'u informuar mbi termat dhe kushtet \n " +
                "Ju lutem vizitoni vioapp.al/terms-and-conditions";

        helper.setText(emailContent);

        mailSender.send(message);
    }


    @PostMapping("/create-by-admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> createUserByAdmin(@RequestBody AdminCreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        Roles targetRoleEnum;
        try {
            targetRoleEnum = Roles.valueOf(request.getRoleName().toUpperCase());
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid roleName provided: " + request.getRoleName(), HttpStatus.BAD_REQUEST);
        }

        Role targetRole = roleRepository.findByName(targetRoleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + targetRoleEnum.name()));

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setUsername(request.getUsername());
        newUser.setSurname(request.getSurname());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(targetRole);

        userRepository.save(newUser);

        return new ResponseEntity<>("User '" + newUser.getUsername() + "' created with role: " + targetRole.getName().name(), HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
        try {
            userDetailsService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}