package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "shared_document")
public class SharedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "original_filename", length = 100)
    private String originalFilename;

    @Size(max = 100)
    @Column(name = "file_path", length = 100)
    private String filePath;

    @Column(name = "upload_timestamp")
    private LocalDateTime uploadTimestamp;

    @Column(name = "price", precision = 7, scale = 2)
    private BigDecimal price;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

    @Size(max = 100)
    @Column(name = "discount_name", length = 100)
    private String discountName;

    @Column(name = "discount")
    private Boolean discount;

    @Column(name = "discount_number")
    private Integer discountNumber;

}