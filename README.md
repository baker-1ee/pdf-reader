# PDF e-book Reader

모바일에서 PDF를 e-book 리더기처럼 읽을 수 있는 모던 웹 애플리케이션

## 프로젝트 구조

- `frontend/` - Vue.js 기반 프론트엔드
- `backend/` - Kotlin + Spring Boot 기반 백엔드
- `PRD.md` - 제품 요구사항 문서

## 개발 환경 설정

### 사전 요구사항

#### macOS
```bash
# Tesseract OCR 엔진 설치 (이미지 기반 PDF 텍스트 추출용)
brew install tesseract tesseract-lang
```

#### Linux (Ubuntu/Debian)
```bash
# Tesseract OCR 엔진 설치
sudo apt-get update
sudo apt-get install tesseract-ocr
sudo apt-get install tesseract-ocr-kor  # 한글 지원
```

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
- Tesseract OCR (이미지 기반 PDF 텍스트 추출)
- Gradle

## 개발 가이드

자세한 설정 방법은 다음 문서를 참고하세요:
- [Frontend Setup](./frontend/FRONTEND_SETUP.md)

## 기능

### PDF 텍스트 추출
- 일반 PDF 텍스트 추출 (PDFBox)
- 이미지 기반 PDF OCR 처리 (Tesseract)
  - 한글 및 영어 텍스트 인식 지원
  - 이미지 품질에 따라 인식률이 달라질 수 있음
