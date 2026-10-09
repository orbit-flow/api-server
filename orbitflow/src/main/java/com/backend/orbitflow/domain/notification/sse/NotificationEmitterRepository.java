package com.backend.orbitflow.domain.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 사용자 uuid별 SSE 연결 (여러 탭·기기 동시 연결 허용)
// TODO: 다중 인스턴스 배포 시 Redis Pub/Sub 등으로 인스턴스 간 전파 필요 (현재는 연결된 인스턴스에서만 전송)
@Slf4j
@Component
public class NotificationEmitterRepository {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String uuid) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(uuid, key -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        Runnable remove = () -> remove(uuid, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // 연결 직후 더미 이벤트를 보내야 일부 프록시·브라우저에서 연결이 확정됨
        send(uuid, emitter, "connect", "connected");
        return emitter;
    }

    public void send(String uuid, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(uuid);
        if (userEmitters == null) {
            return;
        }
        userEmitters.forEach(emitter -> send(uuid, emitter, eventName, data));
    }

    private void send(String uuid, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 전송 실패, 연결 제거: {}", uuid);
            remove(uuid, emitter);
        }
    }

    private void remove(String uuid, SseEmitter emitter) {
        emitters.computeIfPresent(uuid, (key, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
