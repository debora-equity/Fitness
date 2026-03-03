package com.softgenia.playlist.model.dto.user;

import com.softgenia.playlist.model.entity.SharedDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDocumentDto {
    private Integer id;
    private String originalFilename;
    private String downloadUrl;
    private LocalDateTime uploadTimestamp;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Boolean isPaid;
    private Boolean discount;
    private String discountName;
    private Integer discountNumber;

    public UserDocumentDto(SharedDocument document) {
        this.id = document.getId();
        this.originalFilename = document.getOriginalFilename();
        this.uploadTimestamp = document.getUploadTimestamp();
        this.downloadUrl = "/api/profile/me/documents/" + document.getId() + "/download";
        this.price = document.getPrice();
        this.isPaid = document.getIsPaid();
        this.discount = document.getDiscount();
        this.discountName = document.getDiscountName();
        this.discountNumber = document.getDiscountNumber();
        if (Boolean.TRUE.equals(document.getDiscount()) && document.getDiscountNumber() != null
                && document.getPrice() != null) {
            java.math.BigDecimal multiplier = java.math.BigDecimal.ONE.subtract(
                    new java.math.BigDecimal(document.getDiscountNumber()).divide(new java.math.BigDecimal(100)));
            this.discountedPrice = document.getPrice().multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.discountedPrice = document.getPrice();
        }
    }
}
