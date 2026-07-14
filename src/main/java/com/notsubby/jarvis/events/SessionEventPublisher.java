package com.notsubby.jarvis.events;

import com.notsubby.jarvis.model.AssistantTextEvent;
import com.notsubby.jarvis.model.AudioMetadataEvent;
import com.notsubby.jarvis.model.StatusEvent;

public interface SessionEventPublisher {
    void publishAssistantText(AssistantTextEvent event);

    void publishAudioMetadata(AudioMetadataEvent event);

    void publishStatus(StatusEvent event);
}
