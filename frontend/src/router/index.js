import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Reader from '../views/Reader.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/reader',
      name: 'reader',
      component: Reader
    }
  ]
})

export default router
