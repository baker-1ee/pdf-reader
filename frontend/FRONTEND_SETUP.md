# 프론트엔드 개발 환경 세팅 및 실행 가이드

## 1. 프로젝트 초기화

```bash
npm create vite@latest frontend -- --template vue
```
- Vite + Vue 템플릿으로 `frontend` 폴더에 프로젝트 생성

## 2. 의존성 설치

```bash
cd frontend
npm install
```
- 생성된 프로젝트의 의존성 패키지 설치

## 3. vue-router 설치

```bash
npm install vue-router
```
- Vue 라우터 패키지 설치 (페이지 이동 및 SPA 구현을 위해 필요)

## 4. 개발 서버 실행

```bash
npm run dev
```
- 개발 서버 실행 후, 브라우저에서 `http://localhost:5173` 또는 `http://localhost:5174` 접속

## 5. 라우터 및 페이지 구조
- `src/router/index.js` : 라우터 설정(Home, Reader 페이지)
- `src/views/Home.vue` : PDF 업로드 UI
- `src/views/Reader.vue` : Reader 페이지 스켈레톤
- `src/App.vue` : `<router-view />`로 라우터 기반 페이지 렌더링


