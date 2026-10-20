import { useCallback, useEffect, useState } from 'react'
import { fetchDisruptions, fetchStatus, refreshNow } from './api'
import { LINES, lineInfo } from './lines'
import './App.css'

const AUTO_REFRESH_MS = 30_000

function formatTime(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-SG', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function StatusBanner({ status }) {
  if (!status) return null
  const disrupted = status.overall === 'DISRUPTED'
  return (
    <div
      className={`banner ${disrupted ? 'banner-bad' : 'banner-ok'}`}
      data-testid="status-banner"
    >
      {disrupted
        ? `${status.activeCount} active disruption${status.activeCount === 1 ? '' : 's'}`
        : 'All lines running normally'}
    </div>
  )
}

function DisruptionCard({ d }) {
  const info = lineInfo(d.line)
  return (
    <li className={`card ${d.active ? '' : 'card-resolved'}`} data-testid="disruption-card">
      <div className="card-header">
        <span className="line-badge" style={{ background: info.color }}>
          {d.line}
        </span>
        <span className="line-name">{info.name}</span>
        <span className={`severity severity-${d.severity.toLowerCase()}`}>{d.severity}</span>
        {!d.active && <span className="resolved-tag">Resolved</span>}
      </div>
      <p className="message">{d.message}</p>
      <dl className="meta">
        <div>
          <dt>Direction</dt>
          <dd>{d.direction || '-'}</dd>
        </div>
        <div>
          <dt>Stations</dt>
          <dd>{d.stations || '-'}</dd>
        </div>
        <div>
          <dt>First seen</dt>
          <dd>{formatTime(d.firstSeen)}</dd>
        </div>
        {d.resolvedAt && (
          <div>
            <dt>Resolved</dt>
            <dd>{formatTime(d.resolvedAt)}</dd>
          </div>
        )}
      </dl>
      {d.freeBusAvailable && <p className="bus-note">Free bus service available</p>}
    </li>
  )
}

export default function App() {
  const [status, setStatus] = useState(null)
  const [disruptions, setDisruptions] = useState([])
  const [line, setLine] = useState('')
  const [showResolved, setShowResolved] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)

  const load = useCallback(async () => {
    try {
      const [s, list] = await Promise.all([
        fetchStatus(),
        fetchDisruptions({ line, activeOnly: !showResolved }),
      ])
      setStatus(s)
      setDisruptions(list)
      setError('')
      setLastUpdated(new Date())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [line, showResolved])

  useEffect(() => {
    load()
    const timer = setInterval(load, AUTO_REFRESH_MS)
    return () => clearInterval(timer)
  }, [load])

  async function handleRefresh() {
    setLoading(true)
    try {
      await refreshNow()
    } catch (e) {
      setError(e.message)
    }
    await load()
  }

  return (
    <main className="page">
      <header className="top">
        <div>
          <h1>SG Transport Disruption Tracker</h1>
          <p className="subtitle">Live MRT/LRT train service alerts from LTA DataMall</p>
        </div>
        <button onClick={handleRefresh} disabled={loading} data-testid="refresh-button">
          {loading ? 'Refreshing...' : 'Refresh now'}
        </button>
      </header>

      {error && (
        <div className="banner banner-error" role="alert" data-testid="error-banner">
          Could not load data: {error}
        </div>
      )}

      <StatusBanner status={status} />

      <section className="filters">
        <label>
          Line
          <select
            value={line}
            onChange={(e) => setLine(e.target.value)}
            data-testid="line-filter"
          >
            <option value="">All lines</option>
            {LINES.map((l) => (
              <option key={l.code} value={l.code}>
                {l.code} - {l.name}
              </option>
            ))}
          </select>
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={showResolved}
            onChange={(e) => setShowResolved(e.target.checked)}
            data-testid="show-resolved"
          />
          Show resolved
        </label>
        {lastUpdated && (
          <span className="updated" data-testid="last-updated">
            Updated {lastUpdated.toLocaleTimeString('en-SG')}
          </span>
        )}
      </section>

      {status && Object.keys(status.activeByLine).length > 0 && (
        <section className="summary" data-testid="line-summary">
          {Object.entries(status.activeByLine).map(([code, count]) => (
            <span
              key={code}
              className="chip"
              style={{ borderColor: lineInfo(code).color }}
            >
              {code}: {count}
            </span>
          ))}
        </section>
      )}

      {!loading && !error && disruptions.length === 0 && (
        <p className="empty" data-testid="empty-state">
          No {showResolved ? '' : 'active '}disruptions{line ? ` on ${line}` : ''}.
        </p>
      )}

      <ul className="cards">
        {disruptions.map((d) => (
          <DisruptionCard key={d.id} d={d} />
        ))}
      </ul>
    </main>
  )
}
