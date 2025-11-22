// com/softgenia/playlist/service/ShareService.java
package com.softgenia.playlist.service;

import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.WorkoutRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final JavaMailSender mailSender;
    private final WorkoutRepository workoutRepository;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Transactional(readOnly = true) // readOnly because we are only reading data, not changing it
    public void sendWorkoutShareEmail(String recipientEmail, Integer workoutId) {
        // 1. Find the workout to get its name
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found with id: " + workoutId));

        // 2. Construct the direct link to the workout on your frontend
        // Example: http://localhost:3000/workouts/15
        String workoutUrl = frontendBaseUrl + "/workouts/" + workout.getId();

        // 3. Prepare the email content
        String emailSubject = "Check out this workout: " + workout.getName();
        String emailText = "A friend has invited you to check out a workout on our Fitness App!\n\n" +
                "Workout Name: " + workout.getName() + "\n\n" +
                "Click the link below to view the workout:\n" + workoutUrl;

        // 4. Send the email using the robust MimeMessage approach
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(recipientEmail);
            helper.setSubject(emailSubject);
            helper.setText(emailText);

            mailSender.send(message);

        } catch (Exception e) {
            // Log the error and re-throw so the controller can handle it
            throw new RuntimeException("Failed to send workout share email", e);
        }
    }
}
