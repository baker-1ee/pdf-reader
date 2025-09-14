<template>
  <div>
    <h1>PDF e-book Reader</h1>
    <input type="file" accept="application/pdf" @change="onFileChange" />
    <button @click="testMockApi">Test Mock API</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

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
    const result = await response.json();
    router.push({
      path: '/reader',
      query: { content: JSON.stringify(result) }
    });
  } catch (error) {
    console.error('API call failed:', error);
  }
}
</script>
