const BASE = 'http://localhost:8080/api'

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(`${BASE}${path}`, options)
  } catch (error) {
    throw new Error('백엔드 서버에 연결할 수 없습니다. 백엔드(8080) 실행 상태를 확인하세요.')
  }

  let payload = null
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    payload = await res.json()
  } else {
    const text = await res.text()
    payload = text ? { message: text } : null
  }

  if (!res.ok) {
    const message = payload?.message || `요청 실패 (${res.status})`
    throw new Error(message)
  }

  return payload
}

export async function fetchStatus() {
  return request('/index/status')
}

export async function rebuildIndex(rootPath) {
  return request('/index/rebuild', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rootPath })
  })
}

export async function updateIndex(rootPath) {
  return request('/index/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rootPath })
  })
}

export async function deleteIndex() {
  return request('/index', { method: 'DELETE' })
}

export async function search(q, kind = 'all', page = 0, size = 50) {
  const params = new URLSearchParams({ q, kind, page, size })
  return request(`/search?${params.toString()}`)
}

export async function detail(id) {
  return request(`/search/${id}`)
}

export function previewUrl(id) {
  return `${BASE}/files/${id}/preview`
}

export function downloadUrl(id) {
  return `${BASE}/files/${id}/download`
}
