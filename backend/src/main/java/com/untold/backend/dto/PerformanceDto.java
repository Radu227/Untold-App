package com.untold.backend.dto;

import com.untold.backend.model.PerformanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDto {
    private Long id;
    private Long artistId;
    private String artistName;
    private String artistImageUrl;
    private Long stageId;
    private String stageName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private PerformanceStatus status;
    private Boolean hasConflict; // Soft conflict indicator (e.g. artist double booked)
    /** Populated only on update broadcasts — the list of fields that changed. */
    private List<PerformanceChangeDto> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceChangeDto {
        private String field;
        private String oldValue;
        private String newValue;
    }
}
