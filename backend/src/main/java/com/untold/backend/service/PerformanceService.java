package com.untold.backend.service;

import com.untold.backend.dto.PerformanceDto;
import com.untold.backend.dto.PerformanceRequestDto;
import com.untold.backend.exception.ResourceNotFoundException;
import com.untold.backend.model.Artist;
import com.untold.backend.model.MapPin;
import com.untold.backend.model.Performance;
import com.untold.backend.model.PerformanceStatus;
import com.untold.backend.repository.ArtistRepository;
import com.untold.backend.repository.MapPinRepository;
import com.untold.backend.repository.PerformanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ArtistRepository artistRepository;
    private final MapPinRepository mapPinRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.untold.backend.repository.AnnouncementRepository announcementRepository;
    private final org.springframework.kafka.core.KafkaTemplate<Object, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public PerformanceService(PerformanceRepository performanceRepository,
                              ArtistRepository artistRepository,
                              MapPinRepository mapPinRepository,
                              SimpMessagingTemplate messagingTemplate,
                              com.untold.backend.repository.AnnouncementRepository announcementRepository,
                              org.springframework.kafka.core.KafkaTemplate<Object, Object> kafkaTemplate,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.performanceRepository = performanceRepository;
        this.artistRepository = artistRepository;
        this.mapPinRepository = mapPinRepository;
        this.messagingTemplate = messagingTemplate;
        this.announcementRepository = announcementRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PerformanceDto> getAllPerformances(OffsetDateTime start, OffsetDateTime end, Long stageId, Long artistId) {
        List<Performance> performances;

        if (start != null && end != null) {
            performances = performanceRepository.findPerformancesInWindow(start, end);
        } else {
            performances = performanceRepository.findAll();
        }

        // Apply in-memory filters if other optional parameters are provided
        return performances.stream()
                .filter(p -> stageId == null || p.getStage().getId().equals(stageId))
                .filter(p -> artistId == null || p.getArtist().getId().equals(artistId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PerformanceDto getPerformanceById(Long id) {
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance not found with id: " + id));
        return convertToDto(performance);
    }

    public PerformanceDto createPerformance(PerformanceRequestDto request) {
        validateRequest(request, null);

        Artist artist = artistRepository.findById(request.getArtistId())
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + request.getArtistId()));

        MapPin stage = mapPinRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId()));

        Performance performance = Performance.builder()
                .artist(artist)
                .stage(stage)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus() != null ? request.getStatus() : PerformanceStatus.SCHEDULED)
                .build();

        Performance saved = performanceRepository.save(performance);
        PerformanceDto dto = convertToDto(saved);

        // Auto-broadcast lineup update
        try {
            java.time.ZoneId festivalZone = java.time.ZoneId.of("Europe/Bucharest");
            java.time.ZonedDateTime localStart = dto.getStartTime().atZoneSameInstant(festivalZone);
            java.time.ZonedDateTime localEnd = dto.getEndTime().atZoneSameInstant(festivalZone);

            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String day = localStart.getDayOfWeek().toString().toLowerCase();
            day = day.substring(0, 1).toUpperCase() + day.substring(1);
            String content = String.format("Schedule Update: %s has been scheduled on %s from %s to %s on %s!",
                    artist.getName(), stage.getName(),
                    localStart.format(timeFormatter),
                    localEnd.format(timeFormatter),
                    day);
            broadcastLineupChange(content);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PerformanceService.class)
                    .error("Failed to construct create broadcast message", e);
        }

        return dto;
    }

    public PerformanceDto updatePerformance(Long id, PerformanceRequestDto request) {
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance not found with id: " + id));

        validateRequest(request, id);

        Artist artist = artistRepository.findById(request.getArtistId())
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + request.getArtistId()));

        MapPin stage = mapPinRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId()));

        // ── Capture old values BEFORE mutation ───────────────────────────────
        java.time.ZoneId festivalZone = java.time.ZoneId.of("Europe/Bucharest");
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm", java.util.Locale.ENGLISH);

        String oldArtistName  = performance.getArtist().getName();
        String oldStageName   = performance.getStage().getName();
        String oldStartTime   = performance.getStartTime().atZoneSameInstant(festivalZone).format(dateFmt);
        String oldEndTime     = performance.getEndTime().atZoneSameInstant(festivalZone).format(timeFmt);
        PerformanceStatus originalStatus = performance.getStatus();
        // ─────────────────────────────────────────────────────────────────────

        performance.setArtist(artist);
        performance.setStage(stage);
        performance.setStartTime(request.getStartTime());
        performance.setEndTime(request.getEndTime());
        if (request.getStatus() != null) {
            performance.setStatus(request.getStatus());
        }

        Performance saved = performanceRepository.save(performance);
        PerformanceDto dto = convertToDto(saved);

        // ── Build field-level change list ─────────────────────────────────────
        java.util.List<PerformanceDto.PerformanceChangeDto> changes = new java.util.ArrayList<>();

        String newArtistName = saved.getArtist().getName();
        String newStageName  = saved.getStage().getName();
        String newStartTime  = saved.getStartTime().atZoneSameInstant(festivalZone).format(dateFmt);
        String newEndTime    = saved.getEndTime().atZoneSameInstant(festivalZone).format(timeFmt);

        if (!oldArtistName.equals(newArtistName)) {
            changes.add(PerformanceDto.PerformanceChangeDto.builder()
                    .field("Artist").oldValue(oldArtistName).newValue(newArtistName).build());
        }
        if (!oldStageName.equals(newStageName)) {
            changes.add(PerformanceDto.PerformanceChangeDto.builder()
                    .field("Stage").oldValue(oldStageName).newValue(newStageName).build());
        }
        if (!oldStartTime.equals(newStartTime)) {
            changes.add(PerformanceDto.PerformanceChangeDto.builder()
                    .field("Start Time").oldValue(oldStartTime).newValue(newStartTime).build());
        }
        if (!oldEndTime.equals(newEndTime)) {
            changes.add(PerformanceDto.PerformanceChangeDto.builder()
                    .field("End Time").oldValue(oldEndTime).newValue(newEndTime).build());
        }
        if (originalStatus != saved.getStatus()) {
            changes.add(PerformanceDto.PerformanceChangeDto.builder()
                    .field("Status").oldValue(originalStatus.name()).newValue(saved.getStatus().name()).build());
        }

        // Broadcast a copy that carries the change list (the HTTP response DTO stays clean)
        PerformanceDto broadcastDto = PerformanceDto.builder()
                .id(dto.getId())
                .artistId(dto.getArtistId())
                .artistName(dto.getArtistName())
                .artistImageUrl(dto.getArtistImageUrl())
                .stageId(dto.getStageId())
                .stageName(dto.getStageName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .hasConflict(dto.getHasConflict())
                .changes(changes.isEmpty() ? null : changes)
                .build();

        messagingTemplate.convertAndSend("/topic/performances", broadcastDto);

        // Auto-broadcast lineup update with status-aware messages
        try {
            java.time.ZonedDateTime localStart = dto.getStartTime().atZoneSameInstant(festivalZone);
            java.time.ZonedDateTime localEnd = dto.getEndTime().atZoneSameInstant(festivalZone);
            String day = localStart.getDayOfWeek().toString().toLowerCase();
            day = day.substring(0, 1).toUpperCase() + day.substring(1);

            String content;
            PerformanceStatus newStatus = dto.getStatus();

            if (originalStatus != newStatus) {
                switch (newStatus) {
                    case LIVE:
                        content = String.format("🔴 LIVE NOW: %s is now performing live on the %s! Join the crowd! ⚡",
                                artist.getName(), stage.getName());
                        break;
                    case DELAYED:
                        content = String.format("⚠️ DELAYED: %s's set on the %s has been delayed. New scheduled time: %s - %s on %s. Thank you for your patience! ⏳",
                                artist.getName(), stage.getName(),
                                localStart.format(timeFmt),
                                localEnd.format(timeFmt),
                                day);
                        break;
                    case CANCELLED:
                        content = String.format("❌ CANCELLED: We regret to inform you that %s's performance on the %s has been cancelled.",
                                artist.getName(), stage.getName());
                        break;
                    case COMPLETED:
                        content = String.format("✅ COMPLETED: %s's set on the %s is complete! What an incredible show! 🎉",
                                artist.getName(), stage.getName());
                        break;
                    case SCHEDULED:
                    default:
                        content = String.format("📅 RESCHEDULED: %s is scheduled on the %s from %s to %s on %s.",
                                artist.getName(), stage.getName(),
                                localStart.format(timeFmt),
                                localEnd.format(timeFmt),
                                day);
                        break;
                }
            } else {
                // If status didn't change, but it was updated (e.g. time/stage changed)
                content = String.format("📅 RESCHEDULED: %s's set on the %s has been rescheduled. New time: %s - %s on %s.",
                        artist.getName(), stage.getName(),
                        localStart.format(timeFmt),
                        localEnd.format(timeFmt),
                        day);
            }

            broadcastLineupChange(content);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PerformanceService.class)
                    .error("Failed to construct update broadcast message", e);
        }
        
        return dto;
    }

    public void deletePerformance(Long id) {
        Performance p = performanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance not found with id: " + id));
        performanceRepository.delete(p);

        // Auto-broadcast lineup update
        try {
            String content = String.format("Schedule Change: %s's set on %s has been cancelled.",
                    p.getArtist().getName(), p.getStage().getName());
            broadcastLineupChange(content);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PerformanceService.class)
                    .error("Failed to construct delete broadcast message", e);
        }
    }

    private void validateRequest(PerformanceRequestDto request, Long excludeId) {
        if (request.getArtistId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist ID is required");
        }
        if (request.getStageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stage ID is required");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end times are required");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }

        // Validate that the stage pin is actually of type 'STAGE'
        MapPin stage = mapPinRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage pin not found with id: " + request.getStageId()));
        if (!"STAGE".equalsIgnoreCase(stage.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected map pin is not of type STAGE");
        }

        // Hard Block: Overlapping sets on the SAME stage
        List<Performance> overlapsOnStage = performanceRepository.findOverlappingStageSets(
                request.getStageId(),
                request.getStartTime(),
                request.getEndTime(),
                excludeId
        );
        if (!overlapsOnStage.isEmpty()) {
            Performance clashing = overlapsOnStage.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Stage Clash: The stage '%s' already has a performance scheduled (%s) from %s to %s.",
                            stage.getName(),
                            clashing.getArtist().getName(),
                            clashing.getStartTime().toLocalTime(),
                            clashing.getEndTime().toLocalTime()));
        }
    }

    private PerformanceDto convertToDto(Performance p) {
        // Calculate soft conflicts (overlaps for the same artist on other stages)
        List<Performance> artistConflicts = performanceRepository.findOverlappingArtistSets(
                p.getArtist().getId(),
                p.getStartTime(),
                p.getEndTime(),
                p.getId()
        );

        return PerformanceDto.builder()
                .id(p.getId())
                .artistId(p.getArtist().getId())
                .artistName(p.getArtist().getName())
                .artistImageUrl(p.getArtist().getImageUrl())
                .stageId(p.getStage().getId())
                .stageName(p.getStage().getName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .status(p.getStatus())
                .hasConflict(!artistConflicts.isEmpty())
                .build();
    }

    private void broadcastLineupChange(String content) {
        try {
            com.untold.backend.model.Announcement announcement = new com.untold.backend.model.Announcement();
            announcement.setTitle("Lineup Update 📅");
            announcement.setContent(content);
            announcement.setTargetAudience("All Festival Attendees");
            
            com.untold.backend.model.Announcement saved = announcementRepository.save(announcement);
            String jsonPayload = objectMapper.writeValueAsString(saved);
            
            kafkaTemplate.send(com.untold.backend.configs.KafkaConfig.ANNOUNCEMENT_EVENTS_TOPIC, jsonPayload);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PerformanceService.class)
                    .error("Failed to broadcast lineup change announcement", e);
        }
    }
}
