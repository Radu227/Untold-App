package com.untold.backend.controller;

import com.untold.backend.configs.KafkaConfig;
import com.untold.backend.dto.StaffStatsDto;
import com.untold.backend.dto.response.ApiResponse;
import com.untold.backend.model.StaffMember;
import com.untold.backend.repository.StaffMemberRepository;
import com.untold.backend.service.StaffBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class StaffController {

    private final StaffMemberRepository staffRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StaffBroadcaster staffBroadcaster;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StaffController(StaffMemberRepository staffRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           StaffBroadcaster staffBroadcaster) {
        this.staffRepository = staffRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.staffBroadcaster = staffBroadcaster;
    }

    /**
     * Get all staff members.
     */
    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<StaffMember>>> getAllStaff() {
        List<StaffMember> staff = staffRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(staff));
    }

    /**
     * Get dynamic calculations for the 3 widgets.
     */
    @GetMapping("/staff/stats")
    public ResponseEntity<ApiResponse<StaffStatsDto>> getStaffStats() {
        long onDuty = staffRepository.countOnDutyStaff();
        long activeZones = staffRepository.countActiveZones();
        String radioStatus = onDuty > 0 ? "Optimal" : "Offline";
        
        StaffStatsDto stats = new StaffStatsDto(onDuty, activeZones, radioStatus);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Subscribe to real-time staff dispatching stream via SSE.
     */
    @GetMapping(value = "/staff/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToStaffStream(@RequestParam Long userId) {
        return staffBroadcaster.subscribe(userId);
    }

    /**
     * Reallocate a staff member to a new zone and set status to Active.
     */
    @PutMapping("/staff/{id}/reallocate")
    public ResponseEntity<ApiResponse<StaffMember>> reallocateStaff(
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
        
        Optional<StaffMember> optionalStaff = staffRepository.findById(id);
        if (optionalStaff.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Staff member not found"));
        }

        String newZone = payload.get("zone");
        if (newZone == null || newZone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Zone cannot be empty"));
        }

        StaffMember staff = optionalStaff.get();
        staff.setZone(newZone);
        staff.setStatus("Active"); // Set back to Active on reallocation

        StaffMember saved = staffRepository.save(staff);

        // Publish event to Kafka for real-time notification
        if (saved.getUser() != null) {
            try {
                Map<String, Object> event = Map.of(
                    "userId", saved.getUser().getId(),
                    "staffId", saved.getId(),
                    "name", saved.getName(),
                    "role", saved.getRole(),
                    "zone", newZone
                );
                kafkaTemplate.send(KafkaConfig.STAFF_REALLOCATION_TOPIC, objectMapper.writeValueAsString(event));
            } catch (Exception e) {
                System.err.println("Failed to send reallocation Kafka event: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Staff reallocated successfully", saved));
    }
}
