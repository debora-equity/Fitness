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
import org.springframework.security.crypto.password.PasswordEncoder;
import com.softgenia.playlist.model.dto.user.Verify2FaDto;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
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
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            User user = userRepository.findByUsername(loginDto.getUsername()).orElseThrow(
                    () -> new RuntimeException("User not found after authentication"));

            String code = String.format("%06d", new Random().nextInt(999999));
            user.setTwoFactorCode(code);
            user.setTwoFactorCodeExpires(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);
            try {
                send2FaEmail(user.getEmail(), code);
            } catch (Exception e) {
                response.put("message", "Failed to send 2FA email: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            response.put("message", "2FA code sent to your email.");
            response.put("status", "2FA_REQUIRED");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<Map<String, Object>> verify2Fa(@RequestBody Verify2FaDto verify2FaDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userRepository.findByUsername(verify2FaDto.getUsername()).orElseThrow(
                    () -> new RuntimeException("User not found"));

            if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(verify2FaDto.getCode())) {
                response.put("message", "Invalid 2FA code");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (user.getTwoFactorCodeExpires().isBefore(LocalDateTime.now())) {
                response.put("message", "2FA code expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            user.setTwoFactorCode(null);
            user.setTwoFactorCodeExpires(null);
            String sessionId = UUID.randomUUID().toString();
            user.setLastSessionId(sessionId);
            userRepository.save(user);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null,
                    userDetailsService.loadUserByUsername(user.getUsername()).getAuthorities());

            String token = tokenProvider.generateToken(authentication, sessionId);
            String role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                    .orElse(null);

            response.put("token", token);
            response.put("message", "Login successful!");
            response.put("roles", role);
            response.put("name", user.getName());
            response.put("surname", user.getSurname());
            response.put("image", user.getProfileImage());
            response.put("expiresIn", jwtExpirationDate);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("message", "Verification failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
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

        try {
            sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return new ResponseEntity<>("User registered successfully as USER!", HttpStatus.CREATED);
    }

    private void send2FaEmail(String toEmail, String code) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, fromName));
        helper.setTo(toEmail);
        helper.setSubject("Kodi i verifikimit - VIO APP");

        String emailContent = "Kodi juaj i verifikimit është: " + code + "\n\nKy kod është i vlefshëm për 10 minuta.";

        helper.setText(emailContent, false);
        mailSender.send(message);
    }

    private void sendWelcomeEmail(String toEmail, String username) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, fromName));
        helper.setTo(toEmail);
        helper.setSubject("Mirësevini në VIO APP 🤍");

        String emailContent = "Përshëndetje " + username + ",\n\n" +

                "Faleminderit që u regjistruat në VIO APP 🤍\n" +
                "Jam shumë e lumtur që jeni bërë pjesë në platformën time stërvitore.\n\n" +

                "⚠️ Kujdes:\n" +
                "Këto programe mund të sjellin minus disa kg në një periudhë të shkurtër 😉\n" +
                "Prandaj përgatituni mirë – fizikisht dhe mendërisht – sepse kur filloni, rezultatet nuk vonojnë.\n\n" +

                "Për t’ju ndihmuar të zgjidhni programin që ju përshtatet më së miri, ja dy opsionet që ofron VIO APP:\n\n"
                +

                "Opsioni 1: EBOOK – Ideale për fillestare (por jo vetëm)\n" +
                "Ky program është perfekt nëse:\n" +
                "* doni të dobësoheni me një plan të qartë\n" +
                "* nuk keni ose keni pak eksperiencë\n" +
                "* kërkoni organizim pa stres\n\n" +

                "EBOOK-u përfshin program stërvitjeje, program ushqimor dhe udhëzime hap pas hapi.\n\n" +

                "Opsioni 2: Daily Workouts – Strukturë & progres\n" +
                "Ky program është për ju nëse:\n" +
                "* keni eksperiencë mesatare ose të avancuar\n" +
                "* doni stërvitje të përditshme, të strukturuara\n" +
                "* synoni tonifikim, forcim ose rritje muskulore\n\n" +

                "📲 Për suport, këshilla dhe përditësime, ju ftojmë të bëheni pjesë e kanalit tonë në Instagram:\n" +
                "👉 https://www.instagram.com/channel/AbYf3ySh9Qm2W-By/?igsh=NHlqd216cmRkOWlx\n\n" +

                "📞 069 372 0646\n\n" +

                "Jeni gati?\n" +
                "Zgjidhni programin tuaj dhe nisni sot transformimin tuaj me VIO APP 💪✨\n\n" +

                "Suksese,\n" +
                "VIO APP";

        helper.setText(emailContent, false);
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

        return new ResponseEntity<>(
                "User '" + newUser.getUsername() + "' created with role: " + targetRole.getName().name(),
                HttpStatus.CREATED);
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