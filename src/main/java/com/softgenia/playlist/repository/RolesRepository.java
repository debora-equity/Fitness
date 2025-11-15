package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolesRepository  extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(Roles name);
}
