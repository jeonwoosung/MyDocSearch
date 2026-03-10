const BASE = '/api'

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(`${BASE}${path}`, {
      credentials: 'include',
      ...options
    })
  } catch (error) {
    throw new Error('백엔드 서버에 연결할 수 없습니다. 프론트엔드 컨테이너 프록시 설정을 확인하세요.')
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

export async function fetchMe() {
  return request('/auth/me')
}

export async function login(username, password) {
  return request('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
}

export async function logout() {
  return request('/auth/logout', { method: 'POST' })
}

export async function changePassword(newPassword) {
  return request('/auth/change-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newPassword })
  })
}

export async function fetchDrmFiles() {
  return request('/index/drm-files')
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
