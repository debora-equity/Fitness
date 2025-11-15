package com.softgenia.playlist.model.entity;

import com.softgenia.playlist.model.constants.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 100)
    private Roles name;

    public Role(Roles name) {
        this.name = name;
    }

    public Role() {

    }
}