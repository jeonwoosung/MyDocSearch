import { useEffect, useState } from 'react'
import {
  changePassword,
  deleteIndex,
  detail,
  downloadUrl,
  fetchDrmFiles,
  fetchMe,
  fetchStatus,
  login,
  logout,
  rebuildIndex,
  search,
  updateIndex
} from './api'

const DEFAULT_ROOT = '/data/documents'

function LoginScreen({ onLogin, busy, error }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin')

  const submit = async (event) => {
    event.preventDefault()
    await onLogin(username, password)
  }

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={submit}>
        <div className="eyebrow">Mail Search</div>
        <h1>로그인</h1>
        <p className="login-copy">색인 관리와 검색 기능은 인증 후 사용할 수 있습니다.</p>
        <label className="field">
          <span>아이디</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button className="action login-submit" type="submit" disabled={busy}>
          {busy ? '로그인 중...' : '로그인'}
        </button>
      </form>
    </div>
  )
}

function ChangePasswordScreen({ username, onSubmit, busy, error }) {
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      return
    }
    await onSubmit(newPassword)
  }

  const mismatch = confirmPassword.length > 0 && newPassword !== confirmPassword

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={submit}>
        <div className="eyebrow">First Login</div>
        <h1>비밀번호 변경</h1>
        <p className="login-copy">
          기본 계정 <strong>{username}</strong> 으로 로그인했습니다. 계속 진행하려면 새 비밀번호를 설정하세요.
        </p>
        <label className="field">
          <span>새 비밀번호</span>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
          />
        </label>
        <label className="field">
          <span>새 비밀번호 확인</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
          />
        </label>
        {mismatch && <p className="form-error">비밀번호가 일치하지 않습니다.</p>}
        {error && <p className="form-error">{error}</p>}
        <button className="action login-submit" type="submit" disabled={busy || mismatch}>
          {busy ? '변경 중...' : '비밀번호 변경'}
        </button>
      </form>
    </div>
  )
}

export default function App() {
  const [tab, setTab] = useState('search')
  const [authChecked, setAuthChecked] = useState(false)
  const [user, setUser] = useState(null)
  const [authBusy, setAuthBusy] = useState(false)

  const [rootPath, setRootPath] = useState(DEFAULT_ROOT)
  const [status, setStatus] = useState(null)
  const [drmFiles, setDrmFiles] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busyAction, setBusyAction] = useState('')

  const [q, setQ] = useState('')
  const [kind, setKind] = useState('all')
  const [results, setResults] = useState([])
  const [total, setTotal] = useState(0)
  const [selected, setSelected] = useState(null)

  const refreshStatus = async () => {
    const s = await fetchStatus()
    setStatus(s)
  }

  const refreshDrmFiles = async () => {
    const files = await fetchDrmFiles()
    setDrmFiles(files)
  }

  const loadAuthenticatedData = async () => {
    try {
      const [statusResult, drmResult] = await Promise.all([fetchStatus(), fetchDrmFiles()])
      setStatus(statusResult)
      setDrmFiles(drmResult)
      setError('')
    } catch (e) {
      setStatus(null)
      setDrmFiles([])
      setError(e.message || '초기 데이터 조회에 실패했습니다.')
    }
  }

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const me = await fetchMe()
        setUser(me)
        if (!me.mustChangePassword) {
          await loadAuthenticatedData()
        }
      } catch {
        setUser(null)
      } finally {
        setAuthChecked(true)
      }
    }

    bootstrap()
  }, [])

  const onLogin = async (username, password) => {
    setAuthBusy(true)
    try {
      const me = await login(username, password)
      setUser(me)
      setError('')
      setMessage('')
      if (!me.mustChangePassword) {
        await loadAuthenticatedData()
      }
    } catch (e) {
      setError(e.message || '로그인에 실패했습니다.')
    } finally {
      setAuthBusy(false)
      setAuthChecked(true)
    }
  }

  const onChangePassword = async (newPassword) => {
    setAuthBusy(true)
    try {
      const me = await changePassword(newPassword)
      setUser(me)
      setError('')
      await loadAuthenticatedData()
    } catch (e) {
      setError(e.message || '비밀번호 변경에 실패했습니다.')
    } finally {
      setAuthBusy(false)
    }
  }

  const onLogout = async () => {
    setAuthBusy(true)
    try {
      await logout()
    } catch {
      // Ignore logout failures and clear local session view anyway.
    } finally {
      setUser(null)
      setStatus(null)
      setDrmFiles([])
      setResults([])
      setSelected(null)
      setTotal(0)
      setMessage('')
      setError('')
      setAuthBusy(false)
    }
  }

  const onRebuild = async () => {
    setBusyAction('rebuild')
    try {
      const res = await rebuildIndex(rootPath)
      setMessage(`${res.message} (eml: ${res.emlCount}, 첨부: ${res.attachmentCount}, 파일: ${res.fileCount ?? 0})`)
      setError('')
      await loadAuthenticatedData()
    } catch (e) {
      setError(e.message || '재색인에 실패했습니다.')
    } finally {
      setBusyAction('')
    }
  }

  const onUpdate = async () => {
    setBusyAction('update')
    try {
      const res = await updateIndex(rootPath)
      setMessage(`${res.message} (eml: ${res.emlCount}, 첨부: ${res.attachmentCount}, 파일: ${res.fileCount ?? 0})`)
      setError('')
      await loadAuthenticatedData()
    } catch (e) {
      setError(e.message || '증분 갱신에 실패했습니다.')
    } finally {
      setBusyAction('')
    }
  }

  const onDelete = async () => {
    if (!confirm('색인을 모두 삭제하시겠습니까?')) return
    setBusyAction('delete')
    try {
      const res = await deleteIndex()
      setMessage(res.message)
      setResults([])
      setSelected(null)
      setError('')
      await loadAuthenticatedData()
    } catch (e) {
      setError(e.message || '색인 삭제에 실패했습니다.')
    } finally {
      setBusyAction('')
    }
  }

  const onSearch = async () => {
    try {
      const res = await search(q, kind, 0, 100)
      setTotal(res.total)
      setResults(res.items)
      setSelected(null)
      setError('')
    } catch (e) {
      setResults([])
      setTotal(0)
      setSelected(null)
      setError(e.message || '검색에 실패했습니다.')
    }
  }

  const onSelect = async (id) => {
    try {
      const d = await detail(id)
      setSelected(d)
      setError('')
    } catch (e) {
      setSelected(null)
      setError(e.message || '상세 조회에 실패했습니다.')
    }
  }

  if (!authChecked) {
    return <div className="loading-screen">세션 확인 중...</div>
  }

  if (!user) {
    return <LoginScreen onLogin={onLogin} busy={authBusy} error={error} />
  }

  if (user.mustChangePassword) {
    return <ChangePasswordScreen username={user.username} onSubmit={onChangePassword} busy={authBusy} error={error} />
  }

  return (
    <div className="container">
      <div className="topbar">
        <div>
          <h1>EML 색인/검색 시스템</h1>
          <div className="meta">로그인 사용자: {user.username}</div>
        </div>
        <button className="action secondary" onClick={onLogout} disabled={authBusy}>
          로그아웃
        </button>
      </div>

      <div className="tabs">
        <button className={tab === 'manage' ? 'active' : ''} onClick={() => setTab('manage')}>색인 관리</button>
        <button className={tab === 'search' ? 'active' : ''} onClick={() => setTab('search')}>검색</button>
      </div>
      {error && <p className="form-error">{error}</p>}

      {tab === 'manage' && (
        <div className="panel">
          <div className="row">
            <input className="wide" value={rootPath} onChange={(e) => setRootPath(e.target.value)} />
          </div>
          <div className="row">
            <button className="action" onClick={onRebuild} disabled={!!busyAction}>
              {busyAction === 'rebuild' ? '재색인 진행 중...' : '전체 재색인'}
            </button>
            <button className="action" onClick={onUpdate} disabled={!!busyAction}>
              {busyAction === 'update' ? '갱신 진행 중...' : '증분 갱신'}
            </button>
            <button className="action warn" onClick={onDelete} disabled={!!busyAction}>
              {busyAction === 'delete' ? '삭제 중...' : '색인 삭제'}
            </button>
            <button className="action secondary" onClick={loadAuthenticatedData} disabled={!!busyAction}>
              상태 새로고침
            </button>
          </div>
          {busyAction && <p>작업을 처리 중입니다. 첨부파일이 많으면 시간이 오래 걸릴 수 있습니다.</p>}
          {message && <p>{message}</p>}
          {status && (
            <div className="meta">
              <div>기본 루트 경로: {status.rootPath}</div>
              <div>인덱스 경로: {status.indexPath}</div>
              <div>EML 개수: {status.emlCount}</div>
              <div>첨부 개수: {status.attachmentCount}</div>
              <div>문서 파일 개수: {status.fileCount ?? 0}</div>
              <div>최근 색인 시각: {status.lastIndexedAt || '-'}</div>
            </div>
          )}
          <div className="drm-list">
            <h3>DRM 파일 목록 ({drmFiles.length})</h3>
            {drmFiles.length === 0 && <div className="meta">DRM 파일이 없습니다.</div>}
            {drmFiles.map((item) => (
              <div className="item" key={item.id}>
                <div><strong>{item.title}</strong></div>
                <div className="meta">{item.path}</div>
                <div className="meta">{new Date(item.lastModified).toLocaleString()}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'search' && (
        <div className="grid split">
          <div className="panel">
            <div className="row">
              <input
                className="wide"
                placeholder="검색어 입력"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') onSearch()
                }}
              />
              <select value={kind} onChange={(e) => setKind(e.target.value)}>
                <option value="all">전체</option>
                <option value="eml">EML</option>
                <option value="attachment">첨부파일</option>
                <option value="file">문서파일</option>
              </select>
              <button className="action" onClick={onSearch}>검색</button>
            </div>
            <div className="meta">총 {total}건</div>
            <div className="results">
              {results.map((item) => (
                <div className="item" key={item.id} onClick={() => onSelect(item.id)}>
                  <div><strong>[{item.kind}] {item.title}{item.drm ? ' [DRM]' : ''}</strong></div>
                  <div className="meta">{item.subject}</div>
                  <div className="meta">{item.emlPath}</div>
                  <div>{item.snippet}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="panel">
            {!selected && <p>검색 결과를 클릭하면 상세/미리보기가 표시됩니다.</p>}
            {selected && (
              <>
                <h3>{selected.title}</h3>
                <div className="meta">종류: {selected.kind}</div>
                <div className="meta">DRM 파일: {selected.drm ? '예' : '아니오'}</div>
                <div className="meta">제목: {selected.subject}</div>
                <div className="meta">보낸이: {selected.from}</div>
                <div className="meta">받는이: {selected.to}</div>
                <div className="meta">{selected.kind === 'file' ? '원본 파일' : '원본 EML'}: {selected.emlPath}</div>
                {selected.kind === 'attachment' && <div className="meta">첨부명: {selected.attachmentName}</div>}
                <div className="row" style={{ marginTop: 12 }}>
                  <a href={downloadUrl(selected.id)} target="_blank" rel="noreferrer">
                    <button className="action">원본 열기/다운로드</button>
                  </a>
                </div>
                <pre className="preview">{selected.content || '(미리보기 텍스트 없음)'}</pre>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
