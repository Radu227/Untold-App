package com.untold.backend.service;

import com.untold.backend.configs.KafkaConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StaffBroadcaster {
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        
        List<SseEmitter> list = userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        Runnable cleanup = () -> {
            list.remove(emitter);
            if (list.isEmpty()) {
                userEmitters.remove(userId);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            // Confirm subscription
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            cleanup.run();
        }

        return emitter;
    }

    @KafkaListener(topics = KafkaConfig.STAFF_REALLOCATION_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onReallocationEvent(String eventJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(eventJson, Map.class);
            Number userIdNum = (Number) payload.get("userId");
            if (userIdNum == null) return;

            Long userId = userIdNum.longValue();
            List<SseEmitter> list = userEmitters.get(userId);
            if (list == null || list.isEmpty()) return;

            // Fanout event only to the specific staff member's active streams
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("reallocation").data(eventJson));
                } catch (IOException e) {
                    list.remove(emitter);
                }
            }
        } catch (Exception e) {
            System.err.println("Error decoding reallocation event: " + e.getMessage());
        }
    }
}
