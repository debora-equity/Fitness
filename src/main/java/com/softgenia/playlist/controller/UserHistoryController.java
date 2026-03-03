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


    @GetMapping("/continue-watching")
    public ResponseEntity<ContinueWatchingDto> getContinueWatching(Authentication authentication) {
        String username = authentication.getName();
        ContinueWatchingDto lastWatched = historyService.getLastWatchedVideo(username);

        if (lastWatched == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(lastWatched);
    }

    @PostMapping("/progress")
    public ResponseEntity<Void> updateProgress(Authentication authentication, @Valid @RequestBody UpdateVideoProgressDto dto) {
        String username = authentication.getName();
        historyService.updateVideoProgress(username, dto);
        return ResponseEntity.ok().build();
    }
}
