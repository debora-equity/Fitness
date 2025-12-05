package com.softgenia.playlist.model.dto.document;

import com.softgenia.playlist.model.entity.SharedDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersDocumentsDto {
    private Integer id;
    private String originalFilename;
    private String viewUrl;
    private LocalDateTime uploadTimestamp;
    private BigDecimal price;
    private Boolean isUnlocked;
    private Boolean isBlocked;

    public UsersDocumentsDto(SharedDocument document, boolean hasAccess) {
        this.id = document.getId();
        this.originalFilename = document.getOriginalFilename();
        this.uploadTimestamp = document.getUploadTimestamp();
        this.price = document.getPrice();
        this.isBlocked = document.getIsBlocked();
        this.viewUrl = "/api/profile/documents/" + document.getId() + "/view";
        this.isUnlocked = hasAccess;
    }
}
