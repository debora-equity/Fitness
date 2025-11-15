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
            passwordResetService.createAndSendPasswordResetToken(email);
            return ResponseEntity.ok("Password reset link sent to your email.");
        } catch (RuntimeException e) {

            // --- ADD THIS LINE FOR DEBUGGING ---
            System.err.println("!!! Password reset failed with an exception:");
            e.printStackTrace();
            // --- END OF DEBUGGING LINE ---

            // This is the message you are currently seeing
            return ResponseEntity.ok("If an account with that email exists, a password reset link has been sent.");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            passwordResetService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password has been successfully reset.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
