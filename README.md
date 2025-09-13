# PDF e-book Reader

모바일에서 PDF를 e-book 리더기처럼 읽을 수 있는 모던 웹 애플리케이션

## 프로젝트 구조

- `frontend/` - Vue.js 기반 프론트엔드
- `backend/` - Kotlin + Spring Boot 기반 백엔드
- `PRD.md` - 제품 요구사항 문서

## 개발 환경 설정

### 백엔드 실행
```bash
cd backend
./gradlew bootRun
```

### 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

## 기술 스택

### Frontend
- Vue 3 + Vite
- Vue Router
- PDF.js (텍스트 추출)

### Backend
- Kotlin + Spring Boot
- Apache PDFBox (PDF 처리)
- Gradle

## 개발 가이드

자세한 설정 방법은 다음 문서를 참고하세요:
- [Frontend Setup](./frontend/FRONTEND_SETUP.md)
