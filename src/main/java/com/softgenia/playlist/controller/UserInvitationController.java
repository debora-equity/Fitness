package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.user.AcceptInvitationRequest;
import com.softgenia.playlist.model.dto.user.SendInvitationRequest;
import com.softgenia.playlist.service.UserInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class UserInvitationController {

    private final UserInvitationService invitationService;

    // ENDPOINT 1: ADMIN SENDS THE INVITATION
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> sendInvitation(@RequestBody SendInvitationRequest request) {
        try {
            // Pass the whole request object to the service
            invitationService.createAndSendInvitation(request);
            return ResponseEntity.ok("Invitation sent successfully to " + request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ENDPOINT 2: NEW USER ACCEPTS THE INVITATION AND COMPLETES REGISTRATION
    @PostMapping("/accept")
    public ResponseEntity<String> acceptInvitation(@RequestBody AcceptInvitationRequest request) {
        try {
            invitationService.acceptInvitation(request);
            return ResponseEntity.ok("Registration successful! You can now log in.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
