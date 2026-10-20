// Small wrapper around the backend REST API.

async function request(path, options) {
  const res = await fetch(path, options)
  if (!res.ok) {
    throw new Error(`Request to ${path} failed with status ${res.status}`)
  }
  return res.json()
}

export function fetchDisruptions({ line = '', activeOnly = true } = {}) {
  const params = new URLSearchParams()
  if (line) params.set('line', line)
  params.set('activeOnly', String(activeOnly))
  return request(`/api/disruptions?${params.toString()}`)
}

export function fetchStatus() {
  return request('/api/status')
}

export function refreshNow() {
  return request('/api/refresh', { method: 'POST' })
}
