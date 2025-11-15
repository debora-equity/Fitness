package com.softgenia.playlist.service;

import com.softgenia.playlist.model.entity.PasswordResetToken;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.PasswordResetTokenRepository;
import com.softgenia.playlist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createAndSendPasswordResetToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User with email not found: " + userEmail));

        // If a token already exists for this user, delete it before creating a new one
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Create the token
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(tokenString, user);
        tokenRepository.save(passwordResetToken);

        // Send the email
        // In a real app, the URL would come from your frontend's configuration
        String resetUrl = "http://192.168.2.109:5173/log-in" + tokenString;
        String emailText = "To reset your password, please click the link below:\n" + resetUrl;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject("Password Reset Request");
        email.setText(emailText);
        mailSender.send(email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Password reset token has expired.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);


        tokenRepository.delete(resetToken);
    }
}