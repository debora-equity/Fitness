package com.softgenia.playlist.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    // --- Endpoints for Subscribers/Users/Admins ---

    // Accessible to SUPER_ADMIN, ADMIN, USER, SUBSCRIBER
    @GetMapping // /api/playlists
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER', 'SUBSCRIBER')")
    public ResponseEntity<String> getAllPlaylists() {
        return ResponseEntity.ok("All Playlists (Accessible by everyone)");
    }

    // Accessible to SUPER_ADMIN, ADMIN, USER
    @PostMapping // /api/playlists
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<String> createPlaylist(@RequestBody Map<String, String> data) {
        return new ResponseEntity<>("Playlist '" + data.get("name") + "' created!", HttpStatus.CREATED);
    }

    // --- Endpoints only for Admins ---

    // Accessible only to SUPER_ADMIN and ADMIN
    @DeleteMapping("/{id}") // /api/playlists/1
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> deletePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok("Playlist " + id + " deleted by an Admin.");
    }

    // --- Endpoints only for Super Admin ---

    @GetMapping("/system-info") // /api/playlists/system-info
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> getSystemInfo() {
        return ResponseEntity.ok("System Info accessed by Super Admin ONLY.");
    }
}
