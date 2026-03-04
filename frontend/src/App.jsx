import { useEffect, useState } from 'react'
import {
  fetchStatus,
  fetchDrmFiles,
  rebuildIndex,
  updateIndex,
  deleteIndex,
  search,
  detail,
  downloadUrl
} from './api'

const DEFAULT_ROOT = '/data/documents'

export default function App() {
  const [tab, setTab] = useState('search')

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
    try {
      const s = await fetchStatus()
      setStatus(s)
      setError('')
    } catch (e) {
      setStatus(null)
      setError(e.message || '백엔드 상태 조회에 실패했습니다.')
    }
  }

  const refreshDrmFiles = async () => {
    try {
      const files = await fetchDrmFiles()
      setDrmFiles(files)
      setError('')
    } catch (e) {
      setDrmFiles([])
      setError(e.message || 'DRM 파일 목록 조회에 실패했습니다.')
    }
  }

  useEffect(() => {
    refreshStatus()
    refreshDrmFiles()
  }, [])

  const onRebuild = async () => {
    setBusyAction('rebuild')
    try {
      const res = await rebuildIndex(rootPath)
      setMessage(`${res.message} (eml: ${res.emlCount}, 첨부: ${res.attachmentCount}, 파일: ${res.fileCount ?? 0})`)
      setError('')
      refreshStatus()
      refreshDrmFiles()
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
      refreshStatus()
      refreshDrmFiles()
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
      refreshStatus()
      refreshDrmFiles()
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

  return (
    <div className="container">
      <h1>EML 색인/검색 시스템</h1>

      <div className="tabs">
        <button className={tab === 'manage' ? 'active' : ''} onClick={() => setTab('manage')}>색인 관리</button>
        <button className={tab === 'search' ? 'active' : ''} onClick={() => setTab('search')}>검색</button>
      </div>
      {error && <p style={{ color: '#b91c1c', fontWeight: 700 }}>{error}</p>}

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
            <button className="action" onClick={refreshStatus} disabled={!!busyAction}>상태 새로고침</button>
            <button className="action" onClick={refreshDrmFiles} disabled={!!busyAction}>DRM 목록 새로고침</button>
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
