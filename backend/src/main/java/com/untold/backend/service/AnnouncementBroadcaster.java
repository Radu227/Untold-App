package com.untold.backend.service;

import com.untold.backend.configs.KafkaConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges the announcements Kafka topic to web browser and mobile clients using Server-Sent Events (SSE).
 */
@Service
@Lazy(false)
public class AnnouncementBroadcaster {

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            // Send initial ping to confirm subscription is established
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @KafkaListener(topics = KafkaConfig.ANNOUNCEMENT_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onAnnouncementEvent(String eventJson) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("announcement").data(eventJson));
            } catch (IOException e) {
                emitters.remove(emitter); // drop dead connection
            }
        }
    }
}
