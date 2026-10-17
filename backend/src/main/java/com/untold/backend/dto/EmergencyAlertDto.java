package com.untold.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertDto {
    private Long id;
    private String serviceType;
    private Long pinId;
    private String pinName;
    private String status;
    private LocalDateTime createdAt;
}
