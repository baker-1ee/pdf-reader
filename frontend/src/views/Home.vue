<template>
  <div>
    <h1>PDF e-book Reader</h1>
    <input type="file" accept="application/pdf" @change="onFileChange" />
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router';
const router = useRouter();

function onFileChange(e) {
  const file = e.target.files[0];
  if (file) {
    // 실제 파일 처리 로직은 추후 구현
    router.push('/reader');
  }
}
</script>

