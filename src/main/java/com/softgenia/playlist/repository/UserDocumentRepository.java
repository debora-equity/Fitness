package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.SharedDocument;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserDocumentRepository extends JpaRepository<SharedDocument, Integer> {
}
