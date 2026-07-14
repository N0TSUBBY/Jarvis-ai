package com.notsubby.jarvis.controller;

import com.notsubby.jarvis.model.TranscriptEvent;
import com.notsubby.jarvis.service.AudioSessionCoordinator;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AssistantSocketController {

    private final AudioSessionCoordinator coordinator;

    public AssistantSocketController(AudioSessionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @MessageMapping("/transcript")
    public void onTranscript(@Valid TranscriptEvent event) {
        coordinator.handleTranscript(event);
    }
}
