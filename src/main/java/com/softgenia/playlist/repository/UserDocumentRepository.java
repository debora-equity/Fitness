package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserDocumentRepository extends JpaRepository<UserDocument,Integer> {
}
