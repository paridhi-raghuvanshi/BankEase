package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;

    @Column(length = 200)
    private String description;

    public enum RoleName {
        ROLE_CUSTOMER,
        ROLE_BANKER,
        ROLE_ADMIN
    }
}
