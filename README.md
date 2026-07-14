# JARVIS-AI Realtime Starter

Production-oriented starter for a low-latency JARVIS-inspired assistant using Java 21 + Spring Boot + Gemini + React HUD.

## Architecture

```text
 ┌────────────┐      WebSocket (/app/transcript)      ┌──────────────────────────────┐
 │ React HUD  │ ─────────────────────────────────────▶ │ AssistantSocketController     │
 │ (Vite)     │ ◀── /topic/session/{id}/* events ──── │ AudioSessionCoordinator       │
 └────────────┘                                        │  ├─ MockSttService            │
                                                       │  ├─ PromptBuilder             │
                                                       │  ├─ GeminiApiClient           │
                                                       │  └─ MockTtsService            │
                                                       └──────────────┬───────────────┘
                                                                      │
                                                          Gemini 2.5 Flash API
```

## Features

- Java 21, Spring Boot, Maven backend.
- Async turn pipeline (`CompletableFuture` + virtual-thread executor).
- `AudioSessionCoordinator` with per-session short-term memory buffer.
- `PromptBuilder` prepends JARVIS persona system instruction every request.
- `GeminiClient` integration path (model configurable, default `gemini-2.5-flash`).
- STT/TTS abstraction layer with mock implementations:
  - `SttService` + `MockSttService`
  - `TtsService` + `MockTtsService`
- Realtime WebSocket event channels:
  - Incoming transcript: `/app/transcript`
  - Assistant text: `/topic/session/{sessionId}/assistant`
  - Audio metadata: `/topic/session/{sessionId}/audio`
  - Status + latency: `/topic/session/{sessionId}/status`
- HUD frontend scaffold (reactor, transcript panel, response panel, state + latency indicators).
- Structured logs and latency status events for observability hooks.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+

## Configuration

Copy `.env.example` values into your environment.

Required:
- `GEMINI_API_KEY`

Optional:
- `GEMINI_MODEL` (default `gemini-2.5-flash`)
- `BACKEND_PORT` (default `8080`)
- `FRONTEND_PORT` (default `5173`)
- `JARVIS_MEMORY_WINDOW` (default `8`)
- `VITE_BACKEND_URL`, `VITE_WS_ENDPOINT`, `VITE_SESSION_ID`

Backend config is in `/home/runner/work/Jarvis-ai/Jarvis-ai/src/main/resources/application.yml`.

## Run

### 1) Start backend

```bash
cd /home/runner/work/Jarvis-ai/Jarvis-ai
export GEMINI_API_KEY="<your-key>"
mvn spring-boot:run
```

### 2) Start frontend

```bash
cd /home/runner/work/Jarvis-ai/Jarvis-ai/frontend
npm install
npm run dev
```

Open the Vite URL, type transcript text, and submit. The HUD receives realtime assistant + status events.

## Tests

```bash
cd /home/runner/work/Jarvis-ai/Jarvis-ai
mvn test
```

Included tests:
- `PromptBuilderTest`
- `AudioSessionCoordinatorTest`

## Prompt customization

Edit the JARVIS persona prompt at:

- `/home/runner/work/Jarvis-ai/Jarvis-ai/src/main/resources/prompts/jarvis-system-prompt.txt`

`PromptBuilder` always prepends this instruction before user transcript.

## Swapping STT/TTS providers

Current defaults are mock services. To swap providers:

1. Implement `SttService` or `TtsService` in `com.notsubby.jarvis.service.impl`.
2. Mark your implementation as `@Service`.
3. Disable or qualify the mock beans as needed.

## Latency tuning tips

- Keep user transcript chunks short.
- Keep assistant defaults concise (voice-friendly mode).
- Run backend and frontend on low-latency network path.
- Pre-warm providers and reuse client connections.
- Tune memory window (`JARVIS_MEMORY_WINDOW`) to reduce prompt size.

## Roadmap

- Wake word detection (`Hey Jarvis`) with VAD.
- Barge-in/interrupt support during TTS playback.
- Tool execution framework (calendar, browser, local automation).
- Streaming STT + streaming TTS provider integrations.
- Metrics export (Micrometer/OTel) and dashboards.
