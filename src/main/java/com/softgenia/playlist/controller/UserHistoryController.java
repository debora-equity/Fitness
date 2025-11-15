package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.userHistory.ContinueWatchingDto;
import com.softgenia.playlist.model.dto.userHistory.UpdateVideoProgressDto;
import com.softgenia.playlist.service.UserHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class UserHistoryController {
    private final UserHistoryService historyService;

    // ENDPOINT FOR THE HOMEPAGE
    @GetMapping("/continue-watching")
    public ResponseEntity<ContinueWatchingDto> getContinueWatching(Authentication authentication) {
        String username = authentication.getName();
        ContinueWatchingDto lastWatched = historyService.getLastWatchedVideo(username);

        if (lastWatched == null) {
            // It's better to return 204 No Content than 404 Not Found
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(lastWatched);
    }

    // ENDPOINT FOR THE VIDEO PLAYER
    @PostMapping("/progress")
    public ResponseEntity<Void> updateProgress(Authentication authentication, @Valid @RequestBody UpdateVideoProgressDto dto) {
        String username = authentication.getName();
        historyService.updateVideoProgress(username, dto);
        return ResponseEntity.ok().build();
    }
}
