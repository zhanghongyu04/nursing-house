<template>
  <div class="nh-detail-page">
    <section class="panel header-panel">
      <div class="header-left">
        <div class="header-title-wrap">
          <h1 class="page-title">养老院详情</h1>
          <p class="page-subtitle">查看机构老人能力分布、环境照片与入住明细</p>
        </div>
      </div>

      <div class="header-right">
        <el-select
          v-model="selectedSana"
          class="sana-select"
          placeholder="请选择养老院"
          clearable
          @change="onSanaChange"
        >
          <el-option
            v-for="item in sanaOptions"
            :key="item.sanaName"
            :label="item.sanaName"
            :value="item.sanaName"
          />
        </el-select>
        <el-button :loading="pageRefreshing" @click="refreshPageData" :disabled="!selectedSana">
          刷新数据
        </el-button>
      </div>
    </section>

    <section v-if="!selectedSana" class="panel empty-panel">
      <el-empty description="请选择养老院后查看详情">
        <template #image>
          <el-icon :size="96" color="#c6cfdd"><OfficeBuilding /></el-icon>
        </template>
      </el-empty>
    </section>

    <template v-else>
      <section class="panel stats-panel">
        <div class="section-header">
          <h2>自理能力概览</h2>
          <el-tag type="info" effect="plain">总人数 {{ totalCount }} 人</el-tag>
        </div>

        <div class="stats-grid">
          <button
            v-for="item in statsData"
            :key="item.key"
            type="button"
            class="stat-card"
            :class="{ active: careFilter === item.key }"
            @click="onStatCardClick(item.key)"
          >
            <div class="stat-head">
              <span class="stat-chip" :style="{ color: item.color, backgroundColor: item.lightColor }">
                <component :is="item.icon" />
              </span>
              <span class="stat-ratio">{{ item.ratio.toFixed(1) }}%</span>
            </div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-progress">
              <span class="stat-progress-inner" :style="{ width: `${item.ratio}%`, backgroundColor: item.color }"></span>
            </div>
          </button>
        </div>
      </section>

      <section class="content-grid">
        <article class="panel distribution-panel">
          <div class="panel-header">
            <h3>能力分布图</h3>
            <span class="panel-tip">点击概览卡片可联动筛选右侧老人列表</span>
          </div>

          <div class="distribution-body">
            <div class="chart-box">
              <div ref="chartRef" class="chart-canvas"></div>
              <div v-show="chartLoading" class="chart-loading">
                <el-icon class="is-loading" :size="28"><Loading /></el-icon>
                <span>正在加载分布数据...</span>
              </div>
            </div>

            <div class="legend-list">
              <div v-for="item in legendData" :key="item.label" class="legend-item">
                <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
                <span class="legend-label">{{ item.label }}</span>
                <span class="legend-value">{{ item.value }} 人</span>
              </div>
            </div>
          </div>
        </article>

        <article class="panel elder-panel">
          <div class="panel-header">
            <h3>入住老人</h3>
            <el-tag type="info" effect="plain">共 {{ elderPageTotal }} 人</el-tag>
          </div>

          <div class="filter-row">
            <el-radio-group v-model="careFilter" size="small" @change="onCareFilterChange">
              <el-radio-button :label="-1">全部</el-radio-button>
              <el-radio-button v-for="item in CARE_CONFIG" :key="item.key" :label="item.key">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div v-loading="loadingElders" class="elder-content">
            <el-scrollbar v-if="elders.length > 0" class="elder-scroll">
              <div class="elder-list">
                <div v-for="elder in elders" :key="elder.id" class="elder-item">
                  <div class="elder-avatar">{{ getInitial(elder.elderName) }}</div>
                  <div class="elder-main">
                    <div class="elder-name">{{ elder.elderName || '未命名老人' }}</div>
                    <div class="elder-meta">
                      <el-tag :type="getCareTagType(elder.selfCare)" size="small">{{ getCareLabel(elder.selfCare) }}</el-tag>
                      <span class="meta-text">入院时间：{{ elder.inpatientsTime || '--' }}</span>
                    </div>
                  </div>
                  <div class="elder-days">
                    <strong>{{ elder.inpatientDays ?? 0 }}</strong>
                    <span>入住天数</span>
                  </div>
                </div>
              </div>
            </el-scrollbar>
            <el-empty v-else description="暂无老人信息" :image-size="72" />
          </div>

          <div class="elder-footer" v-if="elderPageTotal > 0">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="elderPageTotal"
              :page-size="elderPageSize"
              :current-page="elderPage"
              @current-change="onElderPageChange"
            />
          </div>
        </article>

        <article v-if="canViewSanaImage" class="panel photo-panel">
          <div class="panel-header photo-header">
            <div>
              <h3>环境照片</h3>
              <p class="photo-subtitle">最多上传 {{ MAX_PHOTOS }} 张，当前 {{ photos.length }} 张，剩余 {{ remainSlots }} 张</p>
            </div>
            <el-upload
              v-if="canAddSanaImage"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="onFileChange"
              :multiple="true"
              :disabled="!canPickMore"
            >
              <el-button type="primary" :icon="Plus" :disabled="!canPickMore">上传照片</el-button>
            </el-upload>
          </div>

          <div v-if="pendingList.length > 0" class="pending-box">
            <div class="pending-top">
              <span>待上传 {{ pendingList.length }} 张</span>
              <div class="pending-actions">
                <el-button type="primary" size="small" @click="onUploadConfirm" :loading="uploading">
                  确认上传
                </el-button>
                <el-button size="small" @click="clearPendingList">清空</el-button>
              </div>
            </div>

            <div class="pending-list">
              <div v-for="(item, idx) in pendingList" :key="item.url" class="pending-item">
                <el-image :src="item.url" fit="cover" class="pending-image" />
                <span class="pending-name" :title="item.name">{{ item.name }}</span>
                <el-icon class="pending-remove" @click="removePendingItem(idx)"><Close /></el-icon>
              </div>
            </div>
          </div>

          <div v-loading="loadingPhotos" class="photo-content">
            <div class="photo-grid">
              <div v-for="(photo, idx) in photos" :key="photo.id || photo.imageUrl" class="photo-tile">
                <el-image
                  :src="getPhotoUrl(photo.imageUrl)"
                  fit="cover"
                  :preview-src-list="allPhotoUrls"
                  :initial-index="idx"
                  class="photo-image"
                />
                <div v-if="canDeleteSanaImage" class="photo-mask">
                  <el-button type="danger" :icon="Delete" circle size="small" @click="onDeletePhoto(photo)" />
                </div>
              </div>

              <div v-for="slot in remainSlots" :key="`slot-${slot}`" class="photo-tile photo-placeholder">
                <el-icon :size="28" color="#b9c3d1"><Picture /></el-icon>
                <span>待补充</span>
              </div>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus';
import {
  CircleCheck,
  Close,
  Delete,
  InfoFilled,
  Loading,
  OfficeBuilding,
  Picture,
  Plus,
  Warning,
  CircleClose
} from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { upload } from '@/api/file';
import {
  addSanaImageAPI,
  deleteSanaImageAPI,
  getElderDistributionAPI,
  pageSanaElderListAPI,
  pageSanaImageAPI
} from '@/api/sanaDetail';
import { pageSanatoriumAPI } from '@/api/sanatorium';
import { hasResourcePath } from '@/constants/authRoles';
import { useUserStore } from '@/stores/userStore';

interface SanaOption {
  sanaName: string;
}

interface SanaPhoto {
  id?: number;
  imageUrl: string;
}

interface ElderItem {
  id: number | string;
  elderName: string;
  selfCare: number;
  inpatientsTime?: string;
  inpatientDays?: number;
}

interface PendingItem {
  file: File;
  url: string;
  name: string;
}

interface CareConfigItem {
  key: number;
  label: string;
  color: string;
  lightColor: string;
  icon: any;
}

const MAX_PHOTOS = 6;
const userStore = useUserStore();

const CARE_CONFIG: CareConfigItem[] = [
  { key: 0, label: '能力完好', color: '#5f8fe8', lightColor: '#eaf3ff', icon: CircleCheck },
  { key: 1, label: '轻度失能', color: '#6ea88f', lightColor: '#edf7f2', icon: InfoFilled },
  { key: 2, label: '中度失能', color: '#d39a52', lightColor: '#fdf5eb', icon: Warning },
  { key: 3, label: '重度失能', color: '#c67d69', lightColor: '#fcefe9', icon: Warning },
  { key: 4, label: '完全失能', color: '#a56060', lightColor: '#f8ecec', icon: CircleClose }
];

const selectedSana = ref('');
const sanaOptions = ref<SanaOption[]>([]);
const distribution = ref<Record<string, number>>({});
const careFilter = ref<number>(-1);

const chartRef = ref<HTMLDivElement | null>(null);
const chartLoading = ref(false);
let chartInstance: echarts.ECharts | null = null;

const pageRefreshing = ref(false);

const pendingList = ref<PendingItem[]>([]);
const photos = ref<SanaPhoto[]>([]);
const loadingPhotos = ref(false);
const uploading = ref(false);

const elders = ref<ElderItem[]>([]);
const elderPage = ref(1);
const elderPageSize = ref(10);
const elderPageTotal = ref(0);
const loadingElders = ref(false);

const statsData = computed(() => {
  const total = CARE_CONFIG.reduce((sum, item) => sum + (distribution.value[item.label] || 0), 0);
  return CARE_CONFIG.map((item) => {
    const value = distribution.value[item.label] || 0;
    return {
      ...item,
      value,
      ratio: total > 0 ? (value / total) * 100 : 0
    };
  });
});

const legendData = computed(() =>
  CARE_CONFIG.map((item) => ({
    label: item.label,
    value: distribution.value[item.label] || 0,
    color: item.color
  }))
);

const totalCount = computed(() => statsData.value.reduce((sum, item) => sum + item.value, 0));

const remainSlots = computed(() => Math.max(0, MAX_PHOTOS - photos.value.length));

const canViewSanaImage = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanaImage/page'));
const canAddSanaImage = computed(() =>
  hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanaImage/add')
    && hasResourcePath(userStore.userInfo.resourcePaths, '/web/commonFile/upload')
);
const canDeleteSanaImage = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanaImage/delete'));

const canPickMore = computed(() => canAddSanaImage.value && photos.value.length + pendingList.value.length < MAX_PHOTOS);

const allPhotoUrls = computed(() =>
  photos.value
    .map((item) => getPhotoUrl(item.imageUrl))
    .filter((item) => !!item)
);

const resetPageData = () => {
  distribution.value = {};
  photos.value = [];
  elders.value = [];
  elderPage.value = 1;
  elderPageTotal.value = 0;
  chartLoading.value = false;
  clearPendingList();
  disposeChart();
};

const getCareLabel = (key?: number) => CARE_CONFIG.find((item) => item.key === key)?.label || '未知';

const getCareTagType = (key?: number) => {
  const map = ['primary', 'success', 'warning', 'danger', 'danger'];
  return map[key ?? -1] || 'info';
};

const getInitial = (name?: string) => (name && name.trim() ? name.trim().slice(0, 1) : '?');

const getPhotoUrl = (url: string) => {
  if (!url) {
    return '';
  }
  const marker = '/nursing-home/';
  const idx = url.indexOf(marker);
  if (idx >= 0) {
    return `/api/v1/commonFile/image/${url.slice(idx + marker.length)}`;
  }
  return url;
};

const disposeChart = () => {
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
};

const initChart = () => {
  if (!chartRef.value) {
    return;
  }
  disposeChart();
  chartInstance = echarts.init(chartRef.value);
  renderChart();
};

const renderChart = () => {
  if (!chartInstance) {
    return;
  }
  const data = CARE_CONFIG.map((item) => ({
    name: item.label,
    value: distribution.value[item.label] || 0,
    itemStyle: {
      color: item.color
    }
  }));

  chartInstance.setOption(
    {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}人 ({d}%)'
      },
      series: [
        {
          type: 'pie',
          radius: ['48%', '72%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderColor: '#ffffff',
            borderWidth: 2,
            borderRadius: 4
          },
          label: {
            show: false
          },
          emphasis: {
            scale: true,
            label: {
              show: true,
              formatter: '{b}\n{c}人',
              fontSize: 14,
              fontWeight: 600,
              color: '#253046'
            }
          },
          data
        }
      ]
    },
    true
  );
};

const handleResize = () => {
  chartInstance?.resize();
};

const loadSanatoriums = async () => {
  try {
    const res: any = await pageSanatoriumAPI({ page: 1, pageSize: 100 });
    const records = res?.data?.records || [];
    sanaOptions.value = records
      .filter((item: any) => item?.sanaName)
      .map((item: any) => ({ sanaName: item.sanaName }));
  } catch {
    ElMessage.error('加载养老院列表失败');
  }
};

const loadDistribution = async () => {
  if (!selectedSana.value) {
    return;
  }
  chartLoading.value = true;
  try {
    const res: any = await getElderDistributionAPI(selectedSana.value);
    distribution.value = res?.data || {};
    if (!chartInstance) {
      await nextTick();
      initChart();
    } else {
      renderChart();
    }
    await nextTick();
    chartInstance?.resize();
  } catch {
    ElMessage.error('加载能力分布失败');
  } finally {
    chartLoading.value = false;
  }
};

const loadPhotos = async () => {
  if (!selectedSana.value || !canViewSanaImage.value) {
    photos.value = [];
    return;
  }
  loadingPhotos.value = true;
  try {
    const res: any = await pageSanaImageAPI({
      page: 1,
      pageSize: MAX_PHOTOS,
      sanaName: selectedSana.value
    });
    photos.value = res?.data?.records || [];
  } catch {
    ElMessage.error('加载照片失败');
  } finally {
    loadingPhotos.value = false;
  }
};

const loadElders = async () => {
  if (!selectedSana.value) {
    return;
  }
  loadingElders.value = true;
  try {
    const res: any = await pageSanaElderListAPI({
      page: elderPage.value,
      pageSize: elderPageSize.value,
      sanaName: selectedSana.value,
      selfCare: careFilter.value === -1 ? undefined : careFilter.value
    });
    elders.value = res?.data?.records || [];
    elderPageTotal.value = res?.data?.total || 0;
  } catch {
    ElMessage.error('加载老人信息失败');
  } finally {
    loadingElders.value = false;
  }
};

const refreshPageData = async () => {
  if (!selectedSana.value) {
    return;
  }
  pageRefreshing.value = true;
  try {
    await Promise.all([loadDistribution(), loadPhotos(), loadElders()]);
  } finally {
    pageRefreshing.value = false;
  }
};

const onSanaChange = async () => {
  if (!selectedSana.value) {
    resetPageData();
    return;
  }

  careFilter.value = -1;
  elderPage.value = 1;
  clearPendingList();

  await nextTick();
  initChart();
  await refreshPageData();
};

const onStatCardClick = async (careKey: number) => {
  careFilter.value = careFilter.value === careKey ? -1 : careKey;
  elderPage.value = 1;
  await loadElders();
};

const onCareFilterChange = async (value: string | number | boolean) => {
  careFilter.value = Number(value);
  elderPage.value = 1;
  await loadElders();
};

const onElderPageChange = async (page: number) => {
  elderPage.value = page;
  await loadElders();
};

const removePendingItem = (idx: number) => {
  const [removed] = pendingList.value.splice(idx, 1);
  if (removed?.url) {
    URL.revokeObjectURL(removed.url);
  }
};

const clearPendingList = () => {
  pendingList.value.forEach((item) => {
    if (item.url) {
      URL.revokeObjectURL(item.url);
    }
  });
  pendingList.value = [];
};

const onFileChange = (uploadFile: UploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }

  if (!canPickMore.value) {
    ElMessage.warning(`最多上传 ${MAX_PHOTOS} 张照片`);
    return;
  }

  const duplicated = pendingList.value.some(
    (item) => item.name === raw.name && item.file.size === raw.size
  );
  if (duplicated) {
    ElMessage.warning('该文件已在待上传列表中');
    return;
  }

  pendingList.value.push({
    file: raw,
    url: URL.createObjectURL(raw),
    name: raw.name
  });
};

const onUploadConfirm = async () => {
  if (!selectedSana.value) {
    ElMessage.warning('请先选择养老院');
    return;
  }
  if (!canAddSanaImage.value) {
    ElMessage.warning('当前账号无上传机构照片权限');
    return;
  }
  if (pendingList.value.length === 0) {
    ElMessage.warning('请先选择待上传照片');
    return;
  }

  uploading.value = true;
  try {
    const snapshot = [...pendingList.value];
    for (const item of snapshot) {
      const formData = new FormData();
      formData.append('file', item.file);

      const uploadRes: any = await upload(formData);
      if (uploadRes?.code !== 200 || !uploadRes?.data) {
        throw new Error('文件上传失败');
      }

      const addRes: any = await addSanaImageAPI({
        sanaName: selectedSana.value,
        imageUrl: uploadRes.data
      });
      if (addRes?.code !== 200) {
        throw new Error('图片记录保存失败');
      }
    }

    ElMessage.success(`成功上传 ${pendingList.value.length} 张照片`);
    clearPendingList();
    await loadPhotos();
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败');
  } finally {
    uploading.value = false;
  }
};

const onDeletePhoto = async (photo: SanaPhoto) => {
  if (!canDeleteSanaImage.value) {
    ElMessage.warning('当前账号无删除机构照片权限');
    return;
  }
  try {
    await ElMessageBox.confirm('确定删除该照片吗？删除后无法恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res: any = await deleteSanaImageAPI({
      imageUrl: photo.imageUrl,
      sanaName: selectedSana.value
    });

    if (res?.code === 200) {
      ElMessage.success('删除成功');
      await loadPhotos();
    } else {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消删除时不提示
  }
};

onMounted(async () => {
  await loadSanatoriums();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  clearPendingList();
  disposeChart();
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.nh-detail-page {
  --page-bg: #f3f6fa;
  --panel-bg: #ffffff;
  --panel-border: #d7e0ea;
  --text-primary: #1a2f4d;
  --text-secondary: #5d728f;
  --accent: #3b6fb1;
  --accent-soft: #eaf0f7;
  --shadow-soft: 0 2px 8px rgba(26, 47, 77, 0.06);
  --surface-muted: #f8fafc;
  --surface-subtle: #fbfcfe;
  min-height: calc(100vh - 56px);
  padding: 20px 24px 28px;
  background: var(--page-bg);
}

.panel {
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  box-shadow: var(--shadow-soft);
}

.header-panel {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.page-subtitle {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sana-select {
  width: 320px;
}

.empty-panel {
  margin-top: 16px;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stats-panel {
  margin-top: 16px;
  padding: 16px 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  border: 1px solid #dbe4ef;
  border-radius: 10px;
  background: var(--surface-muted);
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.stat-card:hover {
  border-color: #c4d0dd;
  box-shadow: 0 4px 10px rgba(26, 47, 77, 0.05);
  transform: translateY(-1px);
}

.stat-card.active {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stat-chip {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.stat-ratio {
  font-size: 12px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
}

.stat-label {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-progress {
  margin-top: 10px;
  height: 4px;
  background: #e5eaf0;
  border-radius: 999px;
  overflow: hidden;
}

.stat-progress-inner {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.content-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(560px, 1.8fr) minmax(340px, 1fr);
  grid-template-areas:
    'distribution elder'
    'photo elder';
  gap: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 17px;
  color: var(--text-primary);
  font-weight: 700;
}

.panel-tip {
  color: var(--text-secondary);
  font-size: 12px;
}

.distribution-panel {
  grid-area: distribution;
  padding: 18px;
}

.distribution-body {
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(240px, 1fr);
  gap: 14px;
  align-items: stretch;
}

.chart-box {
  position: relative;
  border: 1px solid #dde5ee;
  border-radius: 10px;
  background: #f7f9fc;
  min-height: 320px;
  overflow: hidden;
}

.chart-loading {
  position: absolute;
  inset: 0;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: var(--text-secondary);
  background: rgba(247, 249, 252, 0.92);
  z-index: 1;
}

.chart-canvas {
  width: 100%;
  height: 320px;
}

.legend-list {
  border: 1px solid #dde5ee;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f7f9fc;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #d9e1ea;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  color: var(--text-secondary);
  font-size: 13px;
}

.legend-value {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 13px;
}

.photo-panel {
  grid-area: photo;
  padding: 18px;
}

.photo-header {
  align-items: flex-start;
}

.photo-subtitle {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.pending-box {
  margin-top: 14px;
  border: 1px solid #d9e1ea;
  border-radius: 10px;
  background: var(--surface-muted);
  padding: 10px;
}

.pending-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.pending-actions {
  display: flex;
  gap: 8px;
}

.pending-list {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.pending-item {
  position: relative;
  width: 88px;
  flex-shrink: 0;
}

.pending-image {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  border: 1px solid #d9e1ea;
}

.pending-name {
  margin-top: 6px;
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pending-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #d54941;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.photo-content {
  margin-top: 14px;
  min-height: 190px;
  border: 1px solid #dde5ee;
  border-radius: 10px;
  padding: 10px;
  background: #f7f9fc;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.photo-tile {
  position: relative;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 16 / 10;
  background: var(--surface-muted);
}

.photo-image {
  width: 100%;
  height: 100%;
}

.photo-mask {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(26, 47, 77, 0.4);
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-tile:hover .photo-mask {
  opacity: 1;
}

.photo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  color: #8ca0bd;
  border-style: dashed;
}

.elder-panel {
  grid-area: elder;
  padding: 18px 18px 12px;
  display: flex;
  flex-direction: column;
  min-height: 760px;
}

.filter-row {
  margin-top: 14px;
}

.elder-content {
  margin-top: 12px;
  flex: 1;
  min-height: 460px;
  border: 1px solid #dde5ee;
  border-radius: 10px;
  background: #f7f9fc;
  padding: 10px;
}

.elder-scroll {
  height: 100%;
}

.elder-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.elder-item {
  display: grid;
  grid-template-columns: 46px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid #d9e1ea;
  border-radius: 10px;
  background: #ffffff;
}

.elder-avatar {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  background: #eaf0f7;
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.elder-name {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.elder-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-text {
  color: var(--text-secondary);
  font-size: 12px;
}

.elder-days {
  text-align: right;
  color: var(--text-secondary);
  min-width: 72px;
}

.elder-days strong {
  display: block;
  color: var(--accent);
  font-size: 22px;
  line-height: 1;
}

.elder-days span {
  font-size: 11px;
  white-space: nowrap;
}

.elder-footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-radio-group) {
  flex-wrap: wrap;
  row-gap: 8px;
}

:deep(.el-button--primary) {
  background: var(--accent);
  border-color: var(--accent);
}

:deep(.el-button--primary.is-plain) {
  color: var(--accent);
  border-color: #c8d6e8;
  background: #f5f8fc;
}

:deep(.el-select__wrapper),
:deep(.el-input__wrapper) {
  border: 1px solid #d7e0ea;
  box-shadow: none;
  background: #ffffff;
}

:deep(.el-tag.el-tag--info) {
  color: #526b8d;
  border-color: #d6dee7;
  background: #f2f5f9;
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: minmax(500px, 1.5fr) minmax(320px, 1fr);
  }

  .distribution-body {
    grid-template-columns: 1fr;
  }

  .elder-panel {
    min-height: 680px;
  }
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      'distribution'
      'elder'
      'photo';
  }

  .elder-panel {
    min-height: auto;
  }

  .elder-content {
    min-height: 380px;
  }
}

@media (max-width: 768px) {
  .nh-detail-page {
    padding: 14px;
  }

  .header-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .header-left {
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .sana-select {
    flex: 1;
    width: auto;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .photo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .elder-footer {
    justify-content: center;
  }
}

@media (max-width: 520px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .header-right {
    flex-direction: column;
    align-items: stretch;
  }

  .photo-grid {
    grid-template-columns: 1fr;
  }

  .elder-item {
    grid-template-columns: 38px 1fr;
  }

  .elder-days {
    grid-column: 2;
    text-align: left;
    display: flex;
    align-items: baseline;
    gap: 6px;
  }

  .elder-days strong {
    font-size: 18px;
  }
}
</style>

