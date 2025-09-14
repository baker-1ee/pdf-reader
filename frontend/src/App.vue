<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <nav v-if="route.path !== '/reader'">
    <router-link to="/">Home</router-link> |
    <router-link to="/reader">Reader</router-link>
  </nav>
  <router-view />
</template>

<style>
nav {
  padding: 30px;
}

nav a {
  font-weight: bold;
  color: #2c3e50;
  text-decoration: none;
  margin: 0 10px;
}

nav a.router-link-exact-active {
  color: #42b883;
}

body {
  margin: 0;
  padding: 0;
}
</style>
