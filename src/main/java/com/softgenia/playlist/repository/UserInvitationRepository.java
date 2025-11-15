package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, Integer> {
    Optional<UserInvitation> findByToken(String token);
    Optional<UserInvitation> findByEmail(String email);
}
