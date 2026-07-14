import { useEffect, useMemo, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import './App.css'

const backendUrl = import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080'
const wsEndpoint = import.meta.env.VITE_WS_ENDPOINT ?? '/ws-assistant'
const defaultSessionId = import.meta.env.VITE_SESSION_ID ?? 'demo-session'

function App() {
  const [sessionId, setSessionId] = useState(defaultSessionId)
  const [transcriptInput, setTranscriptInput] = useState('')
  const [transcripts, setTranscripts] = useState([])
  const [assistantResponses, setAssistantResponses] = useState([])
  const [status, setStatus] = useState('listening')
  const [latency, setLatency] = useState(0)
  const [connectionState, setConnectionState] = useState('disconnected')
  const clientRef = useRef(null)

  const stateLabel = useMemo(() => {
    return {
      listening: 'Listening',
      thinking: 'Thinking',
      speaking: 'Speaking',
      error: 'Error'
    }[status] ?? 'Idle'
  }, [status])

  useEffect(() => {
    const socketUrl = `${backendUrl.replace(/\/$/, '')}${wsEndpoint}`
    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 2000,
      onConnect: () => {
        setConnectionState('connected')
        client.subscribe(`/topic/session/${sessionId}/assistant`, (message) => {
          const payload = JSON.parse(message.body)
          setAssistantResponses((prev) => [payload, ...prev].slice(0, 20))
        })
        client.subscribe(`/topic/session/${sessionId}/status`, (message) => {
          const payload = JSON.parse(message.body)
          setStatus(payload.state)
          setLatency(payload.latencyMs)
        })
      },
      onStompError: () => {
        setConnectionState('error')
      },
      onWebSocketClose: () => {
        setConnectionState('disconnected')
      }
    })

    client.activate()
    clientRef.current = client
    return () => {
      client.deactivate()
    }
  }, [sessionId])

  const submitTranscript = (event) => {
    event.preventDefault()
    const text = transcriptInput.trim()
    if (!text || !clientRef.current?.connected) {
      return
    }

    const payload = { sessionId, text }
    clientRef.current.publish({
      destination: '/app/transcript',
      body: JSON.stringify(payload)
    })
    setTranscripts((prev) => [payload, ...prev].slice(0, 20))
    setTranscriptInput('')
  }

  return (
    <div className="hud-shell">
      <header className="hud-header glass">
        <h1>J.A.R.V.I.S Console</h1>
        <div className="status-grid">
          <StatusPill label="Connection" value={connectionState} />
          <StatusPill label="Mode" value={stateLabel} />
          <StatusPill label="Latency" value={`${latency} ms`} />
        </div>
      </header>

      <main className="hud-main">
        <section className="panel glass reactor-panel">
          <div className={`reactor ${status}`}>
            <div className="core" />
            <div className="ring ring-1" />
            <div className="ring ring-2" />
            <div className="ring ring-3" />
          </div>
          <p className="reactor-text">{stateLabel}</p>
        </section>

        <section className="panel glass transcript-panel">
          <h2>Live Transcript</h2>
          <form onSubmit={submitTranscript} className="transcript-form">
            <input
              value={sessionId}
              onChange={(event) => setSessionId(event.target.value)}
              placeholder="session-id"
              aria-label="Session id"
            />
            <textarea
              value={transcriptInput}
              onChange={(event) => setTranscriptInput(event.target.value)}
              placeholder="Type transcript text to simulate streaming STT..."
              rows={3}
            />
            <button type="submit">Send Transcript</button>
          </form>
          <ul>
            {transcripts.map((entry, index) => (
              <li key={`${entry.text}-${index}`}>{entry.text}</li>
            ))}
          </ul>
        </section>

        <section className="panel glass response-panel">
          <h2>Assistant Responses</h2>
          <ul>
            {assistantResponses.map((entry, index) => (
              <li key={`${entry.turnId}-${index}`}>{entry.text}</li>
            ))}
          </ul>
        </section>
      </main>
    </div>
  )
}

function StatusPill({ label, value }) {
  return (
    <div className="status-pill">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export default App
