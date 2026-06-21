<template>
  <!-- 根布局：按路由元信息决定是否展示全局导航和页脚。 -->
  <Nav v-if="showNavbar" />
  <div class="content-wrapper" :class="{ 'no-navbar': !showNavbar }">
    <main class="page-main" :class="{ 'llm-main': isLlmPage }">
      <RouterView />
    </main>
    <footer class="global-footer" v-if="showFooter">
      © {{ currentYear }} "政-院-护"协同的智慧康养管理系统. All Rights Reserved.
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import Nav from "@/components/Nav.vue";

// 根组件只负责承载全局框架，不直接参与具体业务页面逻辑。
const route = useRoute();
const currentYear = new Date().getFullYear();

const showNavbar = computed(() => route.meta.showNavbar !== false);
const isLlmPage = computed(() => route.path === '/agent' || route.name === 'agent');
const showFooter = computed(() => showNavbar.value);
</script>

<style scoped>
.content-wrapper {
  margin-top: 60px;
  height: calc(100dvh - 60px);
  min-height: calc(100dvh - 60px);
  display: flex;
  flex-direction: column;
}

.content-wrapper.no-navbar {
  margin-top: 0;
  height: 100dvh;
  min-height: 100dvh;
}

.page-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.page-main.llm-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

.global-footer {
  flex-shrink: 0;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(164, 183, 212, 0.28);
  background: linear-gradient(180deg, #fcfdff 0%, #f8fbff 100%);
  color: #5d7396;
  font-size: 13px;
}
</style>
