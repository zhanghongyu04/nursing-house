<template>
  <!-- 首页驾驶舱：左侧地图总览，右侧机构列表与筛选。 -->
  <section class="home-page">
    <div class="dashboard-shell">
      <div class="hero-banner">
        <div>
          <h2>智慧康养管理系统驾驶舱</h2>
          <p>机构分布、床位负载与区域态势一体化监管视图</p>
        </div>
        <el-tag type="primary" effect="plain">实时监控</el-tag>
      </div>

      <StatsCards />

      <div class="dashboard-grid">
        <article ref="mapPanelRef" class="panel panel-map">
          <header class="panel-header">
            <div>
              <h3>{{ mapPanelTitle }}</h3>
              <p>数据口径：区域机构数量</p>
            </div>
            <div class="panel-actions">
              <el-button v-if="canExportRegionStats" type="primary" @click="exportData">导出数据</el-button>
              <el-button @click="printPage">打印</el-button>
            </div>
          </header>

          <div class="quick-metrics">
            <div class="metric-chip">
              <span>机构总数</span>
              <strong>{{ sortedList.length }} 家</strong>
            </div>
            <div class="metric-chip">
              <span>平均入住率</span>
              <strong>{{ avgOccupancyRate }}%</strong>
            </div>
            <div class="metric-chip">
              <span>最高区域</span>
              <strong>{{ topRegionText }}</strong>
            </div>
          </div>

          <div ref="mapRef" class="map-container"></div>
        </article>

        <article class="panel panel-list">
          <header class="panel-header panel-list-header">
            <h3>机构列表（共 {{ sortedList.length }} 家）</h3>
            <div class="list-tools">
              <el-input
                v-model="keyword"
                clearable
                placeholder="搜索机构/地址"
                class="search-input"
              />
              <el-select v-model="sortType" class="sort-select">
                <el-option label="默认排序" value="default" />
                <el-option label="按入住率排序" value="occupancy" />
                <el-option label="按床位数排序" value="beds" />
              </el-select>
            </div>
          </header>

          <el-scrollbar class="list-scroll">
            <div class="list-wrapper">
              <el-card
                v-for="item in pagedList"
                :key="item.id"
                class="list-item"
                shadow="never"
              >
                <div class="item-title-row">
                  <h4>{{ item.sanaName }}</h4>
                  <el-tag effect="plain">{{ inferRegion(item.sanaAddress) }}</el-tag>
                </div>
                <p class="item-address">{{ item.sanaAddress || '暂无地址信息' }}</p>
                <div class="item-data">
                  <span>床位总数: <b>{{ item.bedCount }}</b></span>
                  <span>已用床位: <b>{{ item.bedInUse }}</b></span>
                  <span>入住率: <b>{{ calcOccupancy(item) }}%</b></span>
                  <span>护理人员: <b>{{ item.nursingCount }}</b></span>
                </div>
                <el-progress
                  :show-text="false"
                  :percentage="calcOccupancy(item)"
                  color="#5d8fe8"
                  :stroke-width="6"
                />
              </el-card>
            </div>
          </el-scrollbar>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              background
              layout="total, sizes, prev, pager, next"
              :total="sortedList.length"
              :page-sizes="[8, 10, 12]"
            />
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import { saveAs } from "file-saver";
import * as XLSX from "xlsx";
import { usePanelStore } from "@/stores/panelStore";
import { useSanatoriumStore } from "@/stores/sanatoriumStore";
import { useUserStore } from "@/stores/userStore";
import { hasResourcePath } from "@/constants/authRoles";
import StatsCards from "@/components/StatsCards.vue";

// 首页只消费展示所需字段，避免把后端完整对象直接暴露给地图和导出逻辑。
interface HomeSanatorium {
  id: number;
  sanaName: string;
  sanaAddress: string;
  bedCount: number;
  bedInUse: number;
  nursingCount: number;
}

// 德州区县名称用于地图分布与地址归属推断。
const REGION_NAMES = [
  "德城区",
  "陵城区",
  "乐陵市",
  "禹城市",
  "宁津县",
  "庆云县",
  "临邑县",
  "齐河县",
  "平原县",
  "夏津县",
  "武城县",
];

const panelStore = usePanelStore();
const sanatoriumStore = useSanatoriumStore();
const userStore = useUserStore();

const hasGlobalSanaScope = computed(() => {
  return !userStore.userInfo.sanaId && (userStore.userInfo.sanaScopeIds || []).length === 0;
});

// 页面状态：地图实例、检索条件与分页参数。
const mapRef = ref<HTMLElement | null>(null);
const mapPanelRef = ref<HTMLElement | null>(null);
const keyword = ref("");
const sortType = ref<"default" | "occupancy" | "beds">("default");
const currentPage = ref(1);
const pageSize = ref(10);

let chart: echarts.ECharts | null = null;
let resizeHandler: (() => void) | null = null;

// 不同角色看到的地图标题和区域统计能力不同。
const canViewRegionStats = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, "/web/panel/regionSanaCount"));
const canExportRegionStats = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, "/web/panel/exportRegionStats"));
const mapPanelTitle = computed(() => ((canViewRegionStats.value && hasGlobalSanaScope.value) ? "德州市养老机构分布图" : "本机构分布图"));

const homeList = computed<HomeSanatorium[]>(() => {
  const list = sanatoriumStore.sanatoriumList || [];
  return list.map((item: any) => ({
    id: Number(item.id),
    sanaName: String(item.sanaName || ""),
    sanaAddress: String(item.sanaAddress || ""),
    bedCount: Number(item.bedCount || 0),
    bedInUse: Number(item.bedInUse || 0),
    nursingCount: Number(item.nursingCount || 0),
  }));
});

// 入住率统一按 0-100 取整，避免页面各处重复写计算逻辑。
const calcOccupancy = (item: HomeSanatorium) => {
  if (!item.bedCount) return 0;
  const value = Math.round((item.bedInUse / item.bedCount) * 100);
  return Math.min(100, Math.max(0, value));
};

// 地址中包含区县名称时直接归属到对应区域，否则标记为未识别。
const inferRegion = (address: string) => {
  const matched = REGION_NAMES.find((region) => address?.includes(region));
  return matched || "未识别区域";
};

// 列表支持关键字过滤和两种排序规则。
const sortedList = computed(() => {
  const key = keyword.value.trim();
  const filtered = homeList.value.filter((item) => {
    if (!key) return true;
    return item.sanaName.includes(key) || item.sanaAddress.includes(key);
  });

  const cloned = [...filtered];
  if (sortType.value === "occupancy") {
    return cloned.sort((a, b) => calcOccupancy(b) - calcOccupancy(a));
  }
  if (sortType.value === "beds") {
    return cloned.sort((a, b) => b.bedCount - a.bedCount);
  }
  return cloned;
});

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return sortedList.value.slice(start, start + pageSize.value);
});

const avgOccupancyRate = computed(() => {
  const totalBeds = sortedList.value.reduce((sum, item) => sum + item.bedCount, 0);
  const totalUsed = sortedList.value.reduce((sum, item) => sum + item.bedInUse, 0);
  if (!totalBeds) return 0;
  return Math.round((totalUsed / totalBeds) * 100);
});

// 地图分布优先使用后端聚合结果，角色无权限时退化为前端地址归类统计。
const regionCountMap = computed<Record<string, number>>(() => {
  const fromApi = panelStore.regionSanaCountList || [];
  if (fromApi.length > 0) {
    return fromApi.reduce<Record<string, number>>((acc, item) => {
      const name = item.regionName;
      if (name) acc[name] = Number(item.sanaCount || 0);
      return acc;
    }, {});
  }

  return homeList.value.reduce<Record<string, number>>((acc, item) => {
    const region = inferRegion(item.sanaAddress);
    if (REGION_NAMES.includes(region)) {
      acc[region] = (acc[region] || 0) + 1;
    }
    return acc;
  }, {});
});

const mapData = computed(() =>
  REGION_NAMES.map((name) => ({
    name,
    value: regionCountMap.value[name] || 0,
  }))
);

const topRegionText = computed(() => {
  const maxItem = mapData.value.reduce((max, cur) => (cur.value > max.value ? cur : max), {
    name: "暂无",
    value: 0,
  });
  return `${maxItem.name}（${maxItem.value}家）`;
});

const updateMapOption = () => {
  if (!chart) return;
  const maxValue = Math.max(1, ...mapData.value.map((x) => Number(x.value) || 0));

  chart.setOption({
    tooltip: {
      trigger: "item",
      formatter: (params: any) => {
        const count = Number(params.value || 0);
        return `<div style="font-size:13px;line-height:1.6">
                  <strong>${params.name}</strong><br/>
                  养老机构数量：${count}家
                </div>`;
      },
    },
    visualMap: {
      min: 0,
      max: maxValue,
      text: ["高", "低"],
      orient: "horizontal",
      left: 20,
      bottom: 12,
      calculable: false,
      textStyle: { color: "#4d6ea3" },
      inRange: {
        color: ["#eaf3ff", "#cddffd", "#9cbff5", "#5f8fe8"],
      },
    },
    series: [
      {
        name: "区域机构数",
        type: "map",
        map: "德州",
        roam: false,
        selectedMode: false,
        zoom: 1.08,
        label: {
          show: true,
          color: "#334f79",
          fontSize: 13,
        },
        itemStyle: {
          areaColor: "#eaf3ff",
          borderColor: "#ffffff",
          borderWidth: 1.2,
        },
        emphasis: {
          disabled: false,
          label: { color: "#1f3f70", fontWeight: 600 },
          itemStyle: {
            areaColor: "#9cbff5",
            borderColor: "#ffffff",
          },
          scale: false,
        },
        data: mapData.value,
      },
    ],
  });
};

// 地图 geojson 从前端静态资源加载，完成后注册到 ECharts。
const initMap = async () => {
  if (!mapRef.value) return;
  if (chart) chart.dispose();

  const response = await fetch("/dezhou.json");
  if (!response.ok) throw new Error(`地图数据加载失败: ${response.status}`);
  const mapJson = await response.json();
  echarts.registerMap("德州", mapJson);

  chart = echarts.init(mapRef.value);
  updateMapOption();

  resizeHandler = () => chart?.resize();
  window.addEventListener("resize", resizeHandler);
};

// 导出当前筛选结果，保持和页面所见数据一致。
const exportData = () => {
  const data = sortedList.value.map((item) => ({
    机构名称: item.sanaName,
    地址: item.sanaAddress,
    床位总数: item.bedCount,
    已用床位: item.bedInUse,
    入住率: `${calcOccupancy(item)}%`,
    护理人员: item.nursingCount,
  }));
  const ws = XLSX.utils.json_to_sheet(data);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "养老院列表");
  const wbout = XLSX.write(wb, { bookType: "xlsx", type: "array" });
  saveAs(new Blob([wbout], { type: "application/octet-stream" }), "养老院列表.xlsx");
};

// 打印时单独打开一个纯净窗口，只保留地图模块所需 DOM 和样式。
const printPage = () => {
  if (!mapPanelRef.value) return;

  const printWindow = window.open("", "_blank", "width=1280,height=900");
  if (!printWindow) return;

  const panelHtml = mapPanelRef.value.outerHTML;
  printWindow.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>地图分布模块打印</title>
        <style>
          * {
            box-sizing: border-box;
          }

          body {
            margin: 0;
            padding: 24px;
            font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
            background: #ffffff;
            color: #1a2f4d;
          }

          .panel {
            width: 100%;
            border: 1px solid #d7e0ea;
            border-radius: 8px;
            background: #ffffff;
            padding: 14px;
          }

          .panel-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            gap: 10px;
          }

          .panel-header h3 {
            margin: 0;
            font-size: 20px;
            color: #1a2f4d;
          }

          .panel-header p {
            margin: 6px 0 0;
            font-size: 12px;
            color: #5d728f;
          }

          .panel-actions {
            display: none !important;
          }

          .quick-metrics {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 8px;
            margin: 10px 0;
          }

          .metric-chip {
            border: 1px solid #d9e1ea;
            border-radius: 4px;
            background: #f8fafc;
            padding: 10px 12px;
          }

          .metric-chip span {
            display: block;
            font-size: 12px;
            color: #5d728f;
            margin-bottom: 4px;
          }

          .metric-chip strong {
            font-size: 16px;
            color: #1a2f4d;
          }

          .map-container {
            width: 100%;
            min-height: 620px;
            border: 1px solid #dde5ee;
            border-radius: 6px;
            background: #f7f9fc;
          }
        </style>
      </head>
      <body>
        ${panelHtml}
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.onload = () => {
    printWindow.print();
    printWindow.close();
  };
};

// 过滤条件变化时重置页码，避免落在空页。
watch([keyword, sortType], () => {
  currentPage.value = 1;
});

watch(mapData, () => {
  updateMapOption();
});

onMounted(async () => {
  // 首屏并行加载面板统计、机构列表和区域分布，完成后再初始化地图。
  const tasks = [
    panelStore.fetchNavInfo(),
    sanatoriumStore.fetchSanatoriumPage({ page: 1, pageSize: 1000 }),
  ];
  if (canViewRegionStats.value) {
    tasks.push(panelStore.fetchRegionSanaCount());
  } else {
    panelStore.regionSanaCountList = [];
  }
  await Promise.allSettled(tasks);
  await nextTick();
  try {
    await initMap();
  } catch (err) {
    console.error("初始化地图失败:", err);
  }
});

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener("resize", resizeHandler);
    resizeHandler = null;
  }
  if (chart) {
    chart.dispose();
    chart = null;
  }
});
</script>

<style scoped>
.home-page {
  --page-bg: #f3f6fa;
  --panel-bg: #ffffff;
  --panel-border: #d7e0ea;
  --text-primary: #1a2f4d;
  --text-secondary: #5d728f;
  --accent: #2f5e9e;
  --accent-soft: #eaf0f7;
  --shadow-soft: 0 2px 8px rgba(26, 47, 77, 0.06);
  background: var(--page-bg);
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  padding: 10px 12px 14px;
  box-sizing: border-box;
}

.dashboard-shell {
  width: min(100%, 1540px);
  max-width: calc(100vw - 24px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-banner {
  border: 1px solid #d4dde8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-soft);
  min-height: 96px;
}

.hero-banner h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: clamp(22px, 1.8vw, 26px);
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-banner p {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  padding-top: 10px;
  border-top: 1px solid #e7edf4;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(350px, 0.9fr);
  gap: 10px;
  align-items: start;
  width: 100%;
  min-width: 0;
}

.panel {
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: 100%;
}

.panel-map {
  height: 680px;
  padding: 12px;
}

.panel-list {
  height: 680px;
  padding: 12px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.panel-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  line-height: 1.3;
  font-weight: 700;
}

.panel-header p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.panel-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.panel-actions :deep(.el-button) {
  min-width: 70px;
  border-radius: 4px;
  height: 32px;
  padding: 0 12px;
  font-weight: 500;
}

.panel-actions :deep(.el-button--primary) {
  background: #3b6fb1;
  border-color: #3b6fb1;
}

.quick-metrics {
  margin: 8px 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric-chip {
  border: 1px solid #d9e1ea;
  border-radius: 4px;
  background: #f8fafc;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.metric-chip span {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-chip strong {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.3;
  font-weight: 700;
}

.map-container {
  margin-top: 2px;
  border: 1px solid #dde5ee;
  border-radius: 6px;
  background: #f7f9fc;
  width: 100%;
  flex: 1;
  height: auto;
  min-height: 0;
  overflow: hidden;
}

.panel-list-header {
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.list-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 8px;
  min-width: 0;
}

.search-input :deep(.el-input__wrapper),
.sort-select :deep(.el-select__wrapper) {
  border-radius: 4px;
  min-height: 40px;
  box-shadow: none;
  border: 1px solid #d7e0ea;
  background: #ffffff;
}

.list-scroll {
  margin-top: 4px;
  flex: 1;
  height: auto;
  min-height: 0;
}

.list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}

.list-item {
  border-radius: 6px;
  border: 1px solid #d9e1ea;
  background: #ffffff;
  transition: border-color 180ms ease, box-shadow 180ms ease;
  max-width: 100%;
}

.list-item:hover {
  transform: none;
  border-color: #c4d0dd;
  box-shadow: 0 4px 10px rgba(26, 47, 77, 0.05);
}

.list-item :deep(.el-card__body) {
  padding: 12px 14px;
}

.item-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.item-title-row h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.35;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.item-title-row :deep(.el-tag) {
  border-radius: 999px;
  height: 24px;
  padding: 0 8px;
  color: #526b8d;
  background: #f2f5f9;
  border-color: #d6dee7;
}

.item-address {
  margin: 8px 0 10px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.item-data {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 10px;
  color: #4f6483;
  font-size: 12px;
  margin-bottom: 8px;
}

.item-data b {
  color: var(--text-primary);
}

.list-item :deep(.el-progress-bar__outer) {
  background: #e5eaf0;
  border-radius: 999px;
}

.list-item :deep(.el-progress-bar__inner) {
  border-radius: 999px;
}

.pager-row {
  border-top: 1px solid #e2eaf4;
  margin-top: 6px;
  padding-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.pager-row :deep(.el-pagination) {
  width: 100%;
  justify-content: flex-end;
}

.hero-banner :deep(.el-tag) {
  border-radius: 999px;
  height: 32px;
  padding: 0 14px;
  color: var(--accent);
  background: #f5f8fc;
  border-color: #d7e0ea;
  font-weight: 600;
}

@media (max-width: 1600px) {
  .dashboard-shell {
    width: min(100%, 1460px);
    max-width: calc(100vw - 20px);
  }
}

@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .panel-map,
  .panel-list {
    height: auto;
    min-height: 620px;
  }

  .map-container {
    min-height: 420px;
  }

  .list-scroll {
    min-height: 460px;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 12px;
  }

  .hero-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .hero-banner h2 {
    font-size: 22px;
  }

  .quick-metrics {
    grid-template-columns: 1fr;
  }

  .list-tools {
    grid-template-columns: 1fr;
  }

  .item-data {
    grid-template-columns: 1fr;
  }

  .panel-map,
  .panel-list {
    padding: 12px;
    height: auto;
    min-height: auto;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .panel-actions {
    justify-content: flex-start;
  }

  .map-container {
    min-height: 360px;
  }

  .list-scroll {
    min-height: 420px;
  }
}
</style>

