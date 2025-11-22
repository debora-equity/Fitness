package com.softgenia.playlist.controller;


import com.softgenia.playlist.model.dto.user.ShareWorkoutRequest;
import com.softgenia.playlist.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/workout")

    public ResponseEntity<String> shareWorkout(@Valid @RequestBody ShareWorkoutRequest request) {
        try {
            shareService.sendWorkoutShareEmail(request.getRecipientEmail(), request.getWorkoutId());
            return ResponseEntity.ok("Workout shared successfully with " + request.getRecipientEmail());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
