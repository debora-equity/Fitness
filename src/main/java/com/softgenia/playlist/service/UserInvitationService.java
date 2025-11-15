package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.user.AcceptInvitationRequest;
import com.softgenia.playlist.model.dto.user.SendInvitationRequest;
import com.softgenia.playlist.model.entity.Role;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.UserInvitation;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.RolesRepository;
import com.softgenia.playlist.repository.UserInvitationRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserInvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final WorkoutRepository workoutRepository;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    // --- UPDATED METHOD TO SEND AN INVITATION ---
    @Transactional
    public void createAndSendInvitation(SendInvitationRequest request) {
        String email = request.getEmail();
        Roles role = Roles.valueOf(request.getRoleName().toUpperCase());
        Integer workoutId = request.getWorkoutId();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        invitationRepository.findByEmail(email).ifPresent(invitationRepository::delete);

        String tokenString = UUID.randomUUID().toString();
        UserInvitation invitation;

        // Check if the invitation is for a specific workout
        if (workoutId != null) {
            Workout workout = workoutRepository.findById(workoutId)
                    .orElseThrow(() -> new RuntimeException("Workout not found: " + workoutId));
            invitation = new UserInvitation();
        } else {
            invitation = new UserInvitation(tokenString, email, role);
        }

        invitationRepository.save(invitation);

        // The registration URL remains the same. The token contains all the context.
        String registrationUrl = frontendBaseUrl + "/register?invitationToken=" + tokenString;
        String emailText = "You have been invited to join our Fitness App! " +
                "Please click the link below to complete your registration:\n" + registrationUrl;

        // ... send the email ...
    }

    // --- UPDATED METHOD TO ACCEPT THE INVITATION ---
    @Transactional
    public User acceptInvitation(AcceptInvitationRequest request) {
        UserInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired invitation token."));

        // ... (check expiration, create user, assign role) ...
        // ... all that logic remains the same ...

        User newUser = new User();
        // ... set user properties ...
        newUser.setEmail(invitation.getEmail());
        // ... set password and roles ...

        // --- NEW LOGIC: LINK USER TO THE WORKOUT ---
        if (invitation.getWorkout() != null) {
            Workout workout = invitation.getWorkout();
            // Assuming a Workout can have multiple Users (e.g., members)
            // If a User has a Set<Workout> workouts, you'd do:
            newUser.getWorkouts().add(workout);
        }
        // --- END NEW LOGIC ---

        User savedUser = userRepository.save(newUser);
        invitationRepository.delete(invitation);
        return savedUser;
    }
}