package com.untold.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "staff_members")
@Data
public class StaffMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String dept;

    @Column(nullable = false)
    private String zone;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String avatar;
}
