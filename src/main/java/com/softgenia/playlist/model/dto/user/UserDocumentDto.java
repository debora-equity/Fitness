package com.softgenia.playlist.model.dto.user;

import com.softgenia.playlist.model.entity.UserDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDocumentDto {
    private Integer id;
    private String originalFilename;
    private String downloadUrl;
    private LocalDateTime uploadTimestamp;

    public UserDocumentDto(UserDocument document) {
        this.id = document.getId();
        this.originalFilename = document.getOriginalFilename();
        this.uploadTimestamp = document.getUploadTimestamp();
        this.downloadUrl = "/api/profile/me/documents/" + document.getId() + "/download";
    }
}
