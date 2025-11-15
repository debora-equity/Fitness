package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.user.profile.UpdateUserDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional
    public User updateUserProfile(String username, UpdateUserDto dto, MultipartFile imageFile) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getSurname() != null) user.setSurname(dto.getSurname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());


        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImageUrl = user.getProfileImage();
            String newImageUrl = fileStorageService.saveFile(imageFile);
            user.setProfileImage(newImageUrl);


            if (oldImageUrl != null) {
                fileStorageService.deleteFile(oldImageUrl);
            }
        }

        return userRepository.save(user);
    }
}
