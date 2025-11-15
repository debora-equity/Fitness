package com.softgenia.playlist.model.entity;

import com.softgenia.playlist.model.constants.Roles;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_invitations")
public class UserInvitation {

    private static final int EXPIRATION_DAYS = 7;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Lob
    @Column(name = "token")
    private String token;

    @Lob
    @Column(name = "email")
    private String email;

    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_roles")
    private Roles assignedRoles;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    public UserInvitation(String token, String email, Roles assignedRole) {
        this.token = token;
        this.email = email;
        this.assignedRoles = assignedRole;
        this.expiryDate = LocalDateTime.now().plusDays(EXPIRATION_DAYS);
    }


    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

}