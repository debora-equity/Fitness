package com.softgenia.playlist.controller;

import com.softgenia.playlist.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            passwordResetService.generateAndEmailNewPassword(email);
            return ResponseEntity.ok("New password sent to your email.");
        } catch (RuntimeException e) {
            System.err.println("!!! FAILED TO SEND NEW PASSWORD EMAIL:");
            e.printStackTrace();
            return ResponseEntity.ok("If an account with that email exists, a new password has been sent.");
        }
    }
}
