package com.untold.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffStatsDto {
    private long totalOnDuty;
    private long activeZones;
    private String radioStatus;
}
