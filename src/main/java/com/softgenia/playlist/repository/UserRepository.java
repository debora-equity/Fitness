package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.dto.user.profile.UserFilterDto;
import com.softgenia.playlist.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("""
            select u from User u
            where (:#{#filters.name} is null or u.name like :#{#filters.name})
              and (:#{#filters.surname} is null or u.surname like :#{#filters.surname})
              and (:#{#filters.search} is null or 
                    u.name like :#{#filters.search} 
                    or u.surname like :#{#filters.search})
            """)
    Page<User> getUsers(UserFilterDto filters, Pageable pageable);


    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
