<template>
  <div class="stats-cards">
    <div class="card" v-for="item in formattedStats" :key="item.title">
      <div class="card-top">
        <div class="title">{{ item.title }}</div>
        <span class="suffix">当前</span>
      </div>
      <div class="value">{{ item.value }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted} from 'vue';
import { usePanelStore } from '@/stores/panelStore';
import { storeToRefs } from 'pinia';

// 使用store
const panelStore = usePanelStore();
const { navInfo } = storeToRefs(panelStore);

// 格式化store数据为组件需要的结构
const formattedStats = computed(() => [
  {
    title: '养老院总数',
    value: navInfo.value?.sanaCount || '--',
  },
  {
    title: '入住老人',
    value: navInfo.value?.elderCount?.toLocaleString() || '--'
  },
  {
    title: '床位使用率',
    value: navInfo.value?.useRate ? `${(navInfo.value.useRate * 100).toFixed(2)}%` : '--'
  },
  {
    title: '护理人员',
    value: navInfo.value?.nurseCount || '--'
  }
]);

// 在组件挂载时获取数据
onMounted(() => {
  panelStore.fetchNavInfo();
});
</script>

<style scoped>
.stats-cards {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}
.card {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(26, 47, 77, 0.05);
  border: 1px solid #d7e0ea;
  padding: 14px 18px 16px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 92px;
  overflow: hidden;
}

.card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 0;
  background: transparent;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  color: #5d728f;
  font-size: 12px;
  margin-bottom: 10px;
}
.value {
  font-size: clamp(1.55rem, 1.75vw, 1.95rem);
  font-weight: 700;
  margin-bottom: 2px;
  color: #1a2f4d;
}

.suffix {
  flex-shrink: 0;
  height: 22px;
  line-height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #dce5ef;
  background: #f7f9fc;
  color: #6c7f99;
  font-size: 11px;
}

@media (max-width: 1180px) {
  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .card {
    min-height: 84px;
    padding: 14px 16px;
  }
}
</style>
