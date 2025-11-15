package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.user.profile.UpdateUserDto;
import com.softgenia.playlist.model.dto.user.profile.UserProfileResponseDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponseDto> getCurrentUserProfile(Authentication authentication) {
        User user = userProfileService.findUserByUsername(authentication.getName());
        return ResponseEntity.ok(new UserProfileResponseDto(user));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<UserProfileResponseDto> updateUserProfile(
            Authentication authentication,
            @RequestPart("profileData") @Valid UpdateUserDto dto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            String username = authentication.getName();
            User updatedUser = userProfileService.updateUserProfile(username, dto, imageFile);
            return ResponseEntity.ok(new UserProfileResponseDto(updatedUser));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
