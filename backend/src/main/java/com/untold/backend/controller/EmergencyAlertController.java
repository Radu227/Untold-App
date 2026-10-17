package com.untold.backend.controller;

import com.untold.backend.dto.EmergencyAlertDto;
import com.untold.backend.dto.response.ApiResponse;
import com.untold.backend.model.EmergencyAlert;
import com.untold.backend.model.MapPin;
import com.untold.backend.repository.EmergencyAlertRepository;
import com.untold.backend.repository.MapPinRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class EmergencyAlertController {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final MapPinRepository mapPinRepository;

    public EmergencyAlertController(EmergencyAlertRepository emergencyAlertRepository, MapPinRepository mapPinRepository) {
        this.emergencyAlertRepository = emergencyAlertRepository;
        this.mapPinRepository = mapPinRepository;
    }

    @PostMapping("/api/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyAlertDto>> createAlert(@RequestBody EmergencyAlertDto dto) {
        if (dto.getServiceType() == null || dto.getServiceType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tipul serviciului de urgență este obligatoriu"));
        }
        if (dto.getPinId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ID-ul obiectivului este obligatoriu"));
        }

        Optional<MapPin> pinOpt = mapPinRepository.findById(dto.getPinId());
        if (pinOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Obiectivul selectat nu există în sistem"));
        }

        MapPin pin = pinOpt.get();

        EmergencyAlert alert = new EmergencyAlert();
        alert.setServiceType(dto.getServiceType().toUpperCase());
        alert.setPinId(pin.getId());
        alert.setPinName(pin.getName());
        alert.setStatus("PENDING");
        alert.setCreatedAt(LocalDateTime.now());

        EmergencyAlert savedAlert = emergencyAlertRepository.save(alert);
        EmergencyAlertDto responseDto = convertToDto(savedAlert);

        return ResponseEntity.ok(ApiResponse.success("Alertă trimisă cu succes către autorități!", responseDto));
    }

    @GetMapping("/api/alerts")
    public ResponseEntity<ApiResponse<List<EmergencyAlertDto>>> getAllAlerts() {
        List<EmergencyAlert> alerts = emergencyAlertRepository.findAllByOrderByCreatedAtDesc();
        List<EmergencyAlertDto> dtos = alerts.stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Alerte preluate cu succes", dtos));
    }

    @PutMapping("/api/alerts/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyAlertDto>> resolveAlert(@PathVariable Long id) {
        Optional<EmergencyAlert> alertOpt = emergencyAlertRepository.findById(id);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Alerta selectată nu există"));
        }

        EmergencyAlert alert = alertOpt.get();
        alert.setStatus("RESOLVED");
        EmergencyAlert savedAlert = emergencyAlertRepository.save(alert);
        
        return ResponseEntity.ok(ApiResponse.success("Alerta a fost marcată ca rezolvată cu succes!", convertToDto(savedAlert)));
    }

    private EmergencyAlertDto convertToDto(EmergencyAlert alert) {
        return new EmergencyAlertDto(
                alert.getId(),
                alert.getServiceType(),
                alert.getPinId(),
                alert.getPinName(),
                alert.getStatus(),
                alert.getCreatedAt()
        );
    }
}
