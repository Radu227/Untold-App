package com.untold.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.untold.backend.configs.KafkaConfig;
import com.untold.backend.dto.response.ApiResponse;
import com.untold.backend.model.Announcement;
import com.untold.backend.repository.AnnouncementRepository;
import com.untold.backend.service.AnnouncementBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementBroadcaster announcementBroadcaster;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AnnouncementController(AnnouncementRepository announcementRepository,
                                  AnnouncementBroadcaster announcementBroadcaster,
                                  KafkaTemplate<Object, Object> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.announcementRepository = announcementRepository;
        this.announcementBroadcaster = announcementBroadcaster;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all announcements sorted by creation time descending.
     */
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAllAnnouncements() {
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    /**
     * Broadcast a new announcement.
     * Persists the announcement to the database, publishes to Kafka, and returns the response.
     */
    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> broadcastAnnouncement(@RequestBody Announcement announcement) {
        try {
            // Save to PostgreSQL for history tracking
            Announcement saved = announcementRepository.save(announcement);

            // Serialize entity to JSON string for Kafka
            String jsonPayload = objectMapper.writeValueAsString(saved);

            // Send payload to Kafka. This decouples delivery and triggers the AnnouncementBroadcaster
            kafkaTemplate.send(KafkaConfig.ANNOUNCEMENT_EVENTS_TOPIC, jsonPayload);

            return ResponseEntity.ok(ApiResponse.success("Announcement broadcasted successfully", saved));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AnnouncementController.class)
                    .error("Failed to broadcast announcement", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to broadcast announcement: " + e.getMessage()));
        }
    }

    /**
     * Subscribe to real-time Server-Sent Events (SSE) stream of announcements.
     */
    @GetMapping(value = "/stream/announcements", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnnouncements() {
        return announcementBroadcaster.subscribe();
    }
}
