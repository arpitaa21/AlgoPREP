package com.placementportal.entity;

import jakarta.persistence.*;

/** A role is its own table so a user can later hold several roles (MENTOR, etc.) without a schema change. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // STRING (not ORDINAL): reordering the enum later must never silently change stored data.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;

    protected Role() { }                       // JPA needs a no-arg constructor

    public Role(RoleName name) { this.name = name; }

    public Long getId() { return id; }
    public RoleName getName() { return name; }
}
