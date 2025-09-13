import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import Reader from '../views/Reader.vue';

const routes = [
  { path: '/', component: Home },
  { path: '/reader', component: Reader }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;

