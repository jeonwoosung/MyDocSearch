# EML + Document Index/Search (Spring Boot + React)

## Docker 실행
사전 요구사항: Docker Desktop

### 1) 컨테이너 시작
```bash
cd "/Users/jeon-useong/Documents/New project"
docker compose up -d --build
```

### 2) 접속
- Frontend: http://localhost:5173

기본 로그인 계정:
- 아이디: `admin`
- 비밀번호: `admin`

첫 로그인 시에는 즉시 새 비밀번호를 설정해야 합니다.

### 3) 색인 루트
`docker-compose.yml`에서 아래 호스트 경로를 컨테이너 `/data/documents`로 마운트합니다.
- `/data/owncloud/_data/files/admin/files/Documents`

색인 관리 화면의 루트 경로 입력값은 Docker 실행 시 반드시 `/data/documents`를 사용하세요.

원하면 해당 경로를 다른 폴더로 변경할 수 있습니다.

### 4) 중지
```bash
docker compose down
```

## 내장 DB
- 내장 DB는 프로젝트 루트 [`/Users/jeon-useong/Documents/New project/db`](/Users/jeon-useong/Documents/New%20project/db)에 생성됩니다.
- Git에는 포함되지 않도록 `.gitignore`에 제외되어 있습니다.
- [`/Users/jeon-useong/Documents/New project/stop.sh`](/Users/jeon-useong/Documents/New%20project/stop.sh) 실행 시 DB 백업이 생성되며 최근 3개만 유지합니다.

## 지원 범위
- EML 본문 + 첨부파일 텍스트 색인
- 일반 문서 파일 색인(pdf/docx/xlsx/pptx/txt/csv 등)
- DRM 파일 처리: 파일 시작이 `SCDSA`면 본문 파싱 없이 파일명만 색인 + DRM 표시

## 관리 기능
- 전체 재색인
- 증분 갱신
- 색인 삭제
- 상태 조회(eml/첨부/문서 개수)
- 매일 02:00 자동 증분 색인
- 최종 전체/증분 색인 시각 표시

## 메모리 보호
- 기본값으로 20MB 초과 파일은 본문 추출을 건너뛰고 제목 위주로 색인합니다.
- 본문 추출 텍스트는 최대 200,000자까지만 저장해 대용량 문서로 인한 OOM 가능성을 낮췄습니다.
