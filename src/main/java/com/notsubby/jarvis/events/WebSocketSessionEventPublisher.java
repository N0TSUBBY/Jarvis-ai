package com.notsubby.jarvis.events;

import com.notsubby.jarvis.model.AssistantTextEvent;
import com.notsubby.jarvis.model.AudioMetadataEvent;
import com.notsubby.jarvis.model.StatusEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionEventPublisher implements SessionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketSessionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishAssistantText(AssistantTextEvent event) {
        messagingTemplate.convertAndSend(topic(event.sessionId(), "assistant"), event);
    }

    @Override
    public void publishAudioMetadata(AudioMetadataEvent event) {
        messagingTemplate.convertAndSend(topic(event.sessionId(), "audio"), event);
    }

    @Override
    public void publishStatus(StatusEvent event) {
        messagingTemplate.convertAndSend(topic(event.sessionId(), "status"), event);
    }

    private String topic(String sessionId, String channel) {
        return "/topic/session/" + sessionId + "/" + channel;
    }
}
