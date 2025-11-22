package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_document")
public class UserDocument {
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

}