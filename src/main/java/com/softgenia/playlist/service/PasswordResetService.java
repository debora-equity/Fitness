package com.softgenia.playlist.service;

import com.softgenia.playlist.model.entity.PasswordResetToken;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.PasswordResetTokenRepository;
import com.softgenia.playlist.repository.UserRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Transactional
    public void generateAndEmailNewPassword(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User with email not found: " + userEmail));


        String newPassword = RandomStringUtils.randomAlphanumeric(10);


        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);


        String emailText = "Your password has been reset.\n\n" +
                "Your new temporary password is: " + newPassword + "\n\n" +
                "Please log in and change your password immediately from your profile settings.";

        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(user.getEmail());
            helper.setSubject("Your New Password");
            helper.setText(emailText);

            mailSender.send(message);

        } catch (Exception e) {

            throw new RuntimeException("Failed to send new password email", e);
        }
    }
}