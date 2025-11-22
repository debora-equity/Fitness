package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.user.profile.ChangePasswordDto;
import com.softgenia.playlist.model.dto.user.profile.UpdateUserDto;
import com.softgenia.playlist.model.dto.user.UserSummaryDto;
import com.softgenia.playlist.model.dto.user.profile.UserFilterDto;
import com.softgenia.playlist.model.entity.Role;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.RolesRepository;
import com.softgenia.playlist.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (dto.getUsername() != null && !dto.getUsername().equals(username)) {

            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
            }

            user.setUsername(dto.getUsername());
        }


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

    @Transactional
    public PageResponseDto<UserSummaryDto> getAllUsers(UserFilterDto filterDto,Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        filterDto.formatData();
        Page<User> userPage = userRepository.getUsers(filterDto,pageable);

        return new PageResponseDto<UserSummaryDto>().ofPage(userPage.map(UserSummaryDto::new));
    }

    @Transactional
    public User updateUserRole(Integer userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Roles roleEnum = Roles.valueOf(newRoleName.toUpperCase());
        Role newRole = rolesRepository.findByName(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void changeUserPassword(String username, ChangePasswordDto dto) {
        User user = findUserByUsername(username);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password does not match.");
        }
        String newEncodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.setPassword(newEncodedPassword);

        userRepository.save(user);
    }
}
