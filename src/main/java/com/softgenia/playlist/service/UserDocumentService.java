package com.softgenia.playlist.service;


import com.softgenia.playlist.model.entity.UserDocument;
import com.softgenia.playlist.repository.UserDocumentRepository;
import com.softgenia.playlist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDocumentService {

    private final UserDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public UserDocument uploadDocument(MultipartFile file) throws IOException {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed.");
        }

        String filePath = fileStorageService.saveFile(file);
        fileStorageService.linearizePdf(filePath);
        UserDocument document = new UserDocument();
        document.setOriginalFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setUploadTimestamp(LocalDateTime.now());

        return documentRepository.save(document);
    }


    @Transactional(readOnly = true)
    public List<UserDocument> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserDocument getDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }


    @Transactional
    public void deleteDocument(Integer documentId) {
        UserDocument document = getDocumentById(documentId);

        fileStorageService.deleteFile(document.getFilePath());
        documentRepository.delete(document);
    }
}