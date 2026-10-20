// Singapore MRT/LRT line codes (as used by LTA) with display names and official-style colours.
export const LINES = [
  { code: 'NSL', name: 'North-South', color: '#d42e12' },
  { code: 'EWL', name: 'East-West', color: '#009645' },
  { code: 'CGL', name: 'Changi Airport branch', color: '#009645' },
  { code: 'NEL', name: 'North East', color: '#9900aa' },
  { code: 'CCL', name: 'Circle', color: '#fa9e0d' },
  { code: 'DTL', name: 'Downtown', color: '#005ec4' },
  { code: 'TEL', name: 'Thomson-East Coast', color: '#9d5b25' },
  { code: 'BPL', name: 'Bukit Panjang LRT', color: '#748477' },
  { code: 'SLRT', name: 'Sengkang LRT', color: '#748477' },
  { code: 'PLRT', name: 'Punggol LRT', color: '#748477' },
]

export function lineInfo(code) {
  return LINES.find((l) => l.code === code) ?? { code, name: code, color: '#555555' }
}
