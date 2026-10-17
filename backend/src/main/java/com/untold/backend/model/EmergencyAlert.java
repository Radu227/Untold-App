package com.untold.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_alerts")
@Data
public class EmergencyAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "pin_id", nullable = false)
    private Long pinId;

    @Column(name = "pin_name", nullable = false, length = 255)
    private String pinName;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
