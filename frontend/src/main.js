import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './style.css';

// Mock Service Worker 설정
const setupMockWorker = async () => {
    if (import.meta.env.DEV) {
        console.log('Starting Mock Service Worker...');
        const { worker } = await import('./mocks/browser');
        await worker.start();
        console.log('Mock Service Worker started!');
    }
};

// 앱 초기화 및 마운트
const initializeApp = async () => {
    try {
        await setupMockWorker();
        const app = createApp(App);
        app.use(router);
        app.mount('#app');
    } catch (error) {
        console.error('Failed to initialize app:', error);
    }
};

initializeApp();
