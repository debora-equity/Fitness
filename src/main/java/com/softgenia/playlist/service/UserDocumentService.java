package com.softgenia.playlist.service;


import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.document.CreateDocumentDto;
import com.softgenia.playlist.model.dto.document.UsersDocumentsDto;
import com.softgenia.playlist.model.entity.SharedDocument;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.UserDocumentRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDocumentService {

    private final UserDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserSubscriptionRepository subscriptionRepository;

    private void checkAccess(User user, SharedDocument document) throws AccessDeniedException {
        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;
        if (isAdminOrCreator) return;

        boolean isFree = !Boolean.TRUE.equals(document.getIsPaid()) ||
                (document.getPrice() == null || document.getPrice().compareTo(BigDecimal.ZERO) == 0);
        if (isFree) return;

        boolean hasAccess = subscriptionRepository.hasActiveAccessToDocument(
                user,
                document.getId(),
                LocalDateTime.now()
        );

        if (!hasAccess) {

            throw new AccessDeniedException("You must purchase this document to view it.");
        }
    }

    @Transactional
    public void updatePdfPrice(Integer id, CreateDocumentDto dto) {
        var pdf = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        pdf.setPrice(dto.getPrice());
        pdf.setIsBlocked(dto.getIsBlocked());
    }

    @Transactional
    public SharedDocument uploadDocument(MultipartFile file, BigDecimal price) throws IOException {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed.");
        }

        String filePath = fileStorageService.saveFile(file);
        fileStorageService.linearizePdf(filePath);
        SharedDocument document = new SharedDocument();
        document.setOriginalFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setUploadTimestamp(LocalDateTime.now());
        document.setPrice(price);
        document.setIsBlocked(false);

        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public SharedDocument getDocumentById(Integer documentId, String username) throws AccessDeniedException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getId();

        SharedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        boolean hasAccess = false;

        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;

        if (isAdminOrCreator) {
            hasAccess = true;
        } else {
            int subCount = subscriptionRepository.countDocumentAccess(
                    userId,
                    documentId,
                    LocalDateTime.now()
            );
            if (subCount > 0) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            throw new AccessDeniedException("You must purchase this document to view it.");
        }

        return document;
    }

    @Transactional(readOnly = true)
    public List<UsersDocumentsDto> getAllDocuments(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getId();

        List<SharedDocument> allDocs = documentRepository.findAll();

        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;

        Set<Integer> unlockedDocIds = new HashSet<>();
        if (!isAdminOrCreator) {
            List<Integer> activeIds = subscriptionRepository.findActiveDocumentIds(userId, LocalDateTime.now());
            unlockedDocIds.addAll(activeIds);
        }

        return allDocs.stream().map(doc -> {
            boolean isUnlocked = false;

            if (isAdminOrCreator) {
                isUnlocked = true;
            } else if (unlockedDocIds.contains(doc.getId())) {
                isUnlocked = true;
            }

            return new UsersDocumentsDto(doc, isUnlocked);
        }).toList();
    }

    @Transactional
    public void deleteDocument(Integer documentId) {
        SharedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        documentRepository.delete(document);
        documentRepository.flush();

        fileStorageService.deleteFile(document.getFilePath());
    }
}