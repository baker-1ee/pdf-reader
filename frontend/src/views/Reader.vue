<template>
  <div class="reader-container" @click="toggleControls">
    <div class="reader-content">
      <div v-show="showControls" class="page-controls">
        <button
          @click.stop="prevPage"
          :disabled="currentPage === 1"
          class="nav-button"
        >&lt;</button>
        <button
          @click.stop="nextPage"
          :disabled="currentPage === totalPages"
          class="nav-button"
        >&gt;</button>
      </div>
      <div class="page-content" ref="pageContent">
        <div v-if="currentPageContent" class="text-content">
          {{ currentPageContent }}
        </div>
        <div v-else class="no-content">
          내용을 불러올 수 없습니다.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const pageContent = ref(null);
const content = ref('');
const currentPage = ref(1);
const pages = ref([]);
const showControls = ref(false);
let controlsTimeout;

// 페이지 자동 분할
function splitIntoPages() {
  if (!pageContent.value) return;

  const containerHeight = pageContent.value.clientHeight;
  const tempDiv = document.createElement('div');
  tempDiv.style.cssText = window.getComputedStyle(pageContent.value).cssText;
  tempDiv.style.position = 'absolute';
  tempDiv.style.visibility = 'hidden';
  tempDiv.style.height = 'auto';
  tempDiv.style.width = pageContent.value.clientWidth + 'px';
  tempDiv.classList.add('text-content'); // 텍스트 스타일 적용
  document.body.appendChild(tempDiv);

  pages.value = [];
  let remainingText = content.value;

  while (remainingText.length > 0) {
    tempDiv.textContent = remainingText;

    if (tempDiv.offsetHeight <= containerHeight) {
      pages.value.push(remainingText);
      break;
    }

    // 이진 탐색으로 최적의 분할 지점 찾기
    let start = 0;
    let end = remainingText.length;
    let lastGoodLength = 0;

    while (start <= end) {
      const mid = Math.floor((start + end) / 2);
      tempDiv.textContent = remainingText.substring(0, mid);

      if (tempDiv.offsetHeight <= containerHeight) {
        lastGoodLength = mid;
        start = mid + 1;
      } else {
        end = mid - 1;
      }
    }

    // 단어 단위로 조정
    let splitPoint = lastGoodLength;
    while (splitPoint > 0 && remainingText[splitPoint] !== '\n' && remainingText[splitPoint] !== ' ') {
      splitPoint--;
    }
    splitPoint = splitPoint || lastGoodLength; // 단어 구분자를 찾지 못한 경우

    pages.value.push(remainingText.substring(0, splitPoint));
    remainingText = remainingText.substring(splitPoint).trim();
  }

  document.body.removeChild(tempDiv);
  console.log('Pages created:', pages.value.length);
}

onMounted(() => {
  if (route.query.content) {
    content.value = JSON.parse(route.query.content).content;
    nextTick(() => {
      splitIntoPages();
    });
  }

  // 화면 크기 변경 시 페이지 재계산
  window.addEventListener('resize', () => {
    splitIntoPages();
  });
});

// 총 페이지 수 계산
const totalPages = computed(() => pages.value.length);

// 현재 페이지 컨텐츠 가져오기
const currentPageContent = computed(() => {
  console.log('Current page:', currentPage.value, 'of', totalPages.value);
  return pages.value[currentPage.value - 1] || '';
});

// 페이지 네비게이션
function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--;
    console.log('Moved to previous page:', currentPage.value);
  }
}

function nextPage() {
  console.log('Attempting to move to next page. Current:', currentPage.value, 'Total:', totalPages.value);
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    console.log('Moved to next page:', currentPage.value);
  }
}

// 컨트롤 표시/숨김 관리
function toggleControls() {
  showControls.value = true;
  clearTimeout(controlsTimeout);
  controlsTimeout = setTimeout(() => {
    showControls.value = false;
  }, 3000);
}
</script>

<style scoped>
.reader-container {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  background-color: #1a1a1a;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
}

.reader-content {
  width: 100%;
  height: 100%;
  background: #222;
  padding: 3rem;
  padding-bottom: 3rem;
  display: flex;
  flex-direction: column;
  position: relative;
  box-sizing: border-box;
}

.page-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  height: 100%;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.text-content {
  font-size: 1.25rem;
  line-height: 2;
  color: #e0e0e0;
  white-space: pre-wrap;
  font-family: -apple-system, 'Noto Sans KR', sans-serif;
  height: 100%;
  padding: 0.5rem;
  margin: 0;
  text-align: left;
  letter-spacing: 0.01em;
}

.page-controls {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem;
  z-index: 1000;
}

.nav-button {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 22px;
  font-size: 1.4rem;
  cursor: pointer;
  pointer-events: auto;
  opacity: 0.8;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  backdrop-filter: blur(4px);
}

.nav-button:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}

.nav-button:disabled {
  opacity: 0.2;
  cursor: not-allowed;
  transform: none;
}

.no-content {
  text-align: center;
  color: #999;
  padding: 1rem;
}

/* 모바일 최적화 */
@media (max-width: 375px) {
  .reader-content {
    padding: 2rem;
  }

  .text-content {
    font-size: 1.1rem;
    line-height: 1.8;
    padding: 0.25rem;
  }

  .nav-button {
    width: 36px;
    height: 36px;
    font-size: 1.2rem;
  }
}
</style>
