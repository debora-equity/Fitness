package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.document.CreateDocumentDto;
import com.softgenia.playlist.model.dto.document.UsersDocumentsDto;
import com.softgenia.playlist.model.dto.user.UserDocumentDto;
import com.softgenia.playlist.model.dto.user.UserSummaryDto;
import com.softgenia.playlist.model.dto.user.profile.*;
import com.softgenia.playlist.model.entity.SharedDocument;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.service.UserDocumentService;
import com.softgenia.playlist.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final UserDocumentService documentService;

    @Value("${upload.path}")
    private String uploadPath;


    @GetMapping
    public ResponseEntity<UserProfileResponseDto> getCurrentUserProfile(Authentication authentication) {
        User user = userProfileService.findUserByUsername(authentication.getName());
        return ResponseEntity.ok(new UserProfileResponseDto(user));
    }

    @PutMapping(value = "/me", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> updateUserProfile(

            Authentication authentication,
            @RequestPart("profileData") @Valid UpdateUserDto dto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            String currentUsername = authentication.getName();
            User updatedUser = userProfileService.updateUserProfile(currentUsername, dto, imageFile);

            Map<String, Object> response = new HashMap<>();
            response.put("user", new UserProfileResponseDto(updatedUser));

            if (dto.getUsername() != null && !dto.getUsername().equals(currentUsername)) {
                response.put("reauthenticate", true);
                response.put("message", "Username changed successfully. Please log in again.");
            } else {
                response.put("reauthenticate", false);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponseDto<UserSummaryDto>> getAllUsers(
            @RequestParam Integer pageNumber,
            @RequestParam Integer pageSize,
            @ModelAttribute UserFilterDto filterDto) {

        PageResponseDto<UserSummaryDto> users = userProfileService.getAllUsers(filterDto, pageNumber, pageSize);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserSummaryDto> updateUserRole(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRole dto) {

        User updatedUser = userProfileService.updateUserRole(userId, dto.getRoleName());
        return ResponseEntity.ok(new UserSummaryDto(updatedUser));
    }

    @PostMapping("/password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordDto dto) {
        try {
            String username = authentication.getName();
            userProfileService.changeUserPassword(username, dto);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @PostMapping(value = "/documents", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("price") BigDecimal price
    ) {
        try {
            SharedDocument savedDocument = documentService.uploadDocument(file, price);
            return new ResponseEntity<>(new UserDocumentDto(savedDocument), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/documents/{id}/price")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<Void> updateDocumentPrice(
            @PathVariable Integer id,
            @RequestBody CreateDocumentDto price
    ) {
        try {
            documentService.updatePdfPrice(id, price);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/documents")
    public ResponseEntity<List<UsersDocumentsDto>> getAllDocuments(Authentication authentication) {
        String username = authentication.getName();
        List<UsersDocumentsDto> documents = documentService.getAllDocuments(username);

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{documentId}/view")
    public ResponseEntity<?> viewDocument(Authentication authentication, @PathVariable Integer documentId) {
        try {
            String username = authentication.getName();

            SharedDocument document = documentService.getDocumentById(documentId, username);

            Path filePath = Paths.get(uploadPath).resolve(Paths.get(document.getFilePath()).getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Could not read the file!");
            }

            CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS).mustRevalidate();

            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<?> deleteDocument(@PathVariable Integer documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete this document because it has been purchased by users."));

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
