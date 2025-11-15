package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.constants.Roles; // Assuming your Role Enum is named Roles
import com.softgenia.playlist.model.dto.user.LoginDto;
import com.softgenia.playlist.model.dto.user.RegisterDto;
import com.softgenia.playlist.model.dto.user.AdminCreateUserRequest; // You need this DTO
import com.softgenia.playlist.model.entity.Role;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.RolesRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RolesRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> authenticateUser(@RequestBody LoginDto loginDto) {
        Map<String, String> response = new HashMap<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            response.put("token", token);
            response.put("message", "Login successful!");

            return ResponseEntity.ok(response);

        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            response.put("message", "Invalid username or password!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            response.put("message", "User not found!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception ex) {
            response.put("message", "Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterDto registerDto) {

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            return new ResponseEntity<>("Email is already used!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(registerDto.getName());
        user.setUsername(registerDto.getUsername());
        user.setSurname(registerDto.getSurname());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));


        Role defaultRole = roleRepository.findByName(Roles.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Default ROLE_USER not found."));
        user.setRoles(Set.of(defaultRole));


        userRepository.save(user);

        return new ResponseEntity<>("User registered successfully as USER!", HttpStatus.CREATED);
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
        newUser.setRoles(Set.of(targetRole));

        userRepository.save(newUser);

        return new ResponseEntity<>("User '" + newUser.getUsername() + "' created with role: " + targetRole.getName().name(), HttpStatus.CREATED);
    }
}