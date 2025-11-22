package com.softgenia.playlist.service;

import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.PasswordResetTokenRepository;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import jakarta.transaction.Transactional;
import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final WorkoutRepository workoutRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, UserHistoryRepository userHistoryRepository, WorkoutRepository workoutRepository, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.userHistoryRepository = userHistoryRepository;
        this.workoutRepository = workoutRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(loginIdentifier)
                .orElseGet(() -> userRepository.findByEmail(loginIdentifier)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found with username or email: " + loginIdentifier
                        ))
                );
        Collection<? extends GrantedAuthority> authorities = mapRolesToAuthorities(user);


        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }


    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(User user) {

        if (user.getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(user.getRole().getName().name()));
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String currentAdminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentAdminUsername)) {
            throw new IllegalArgumentException("Admins cannot delete their own account.");
        }
        user.getWorkouts().clear();

        userHistoryRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);

        userRepository.delete(user);
    }
}
