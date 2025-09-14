<template>
  <div>
    <h1>PDF e-book Reader</h1>
    <input type="file" accept="application/pdf" @change="onFileChange" />
    <button @click="testMockApi">Test Mock API</button>
    <pre v-if="result">{{ result }}</pre>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const result = ref(null);

function onFileChange(e) {
  const file = e.target.files[0];
  if (file) {
    router.push('/reader');
  }
}

async function testMockApi() {
  try {
    const response = await fetch('/api/documents/extract', {
      method: 'POST',
      body: new FormData()
    });
    result.value = await response.json();
  } catch (error) {
    console.error('API call failed:', error);
  }
}
</script>
