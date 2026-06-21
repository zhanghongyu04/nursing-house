<template>
  <div class="outer-service-control">
    <div class="service-control">
      <!-- CPU 信息和内存信息 -->
      <div class="info-container">
        <!-- CPU 信息 -->
        <div class="info-box cpu-info">
          <div class="info-header">
            <img src="@/assets/images/img/CPU.png"> CPU
            <span class="status-tag" :class="monitorStatus.cpu.level">{{ monitorStatus.cpu.text }}</span>
          </div>
          <div class="info-content">
            <table>
              <thead>
              <tr>
                <th class="info-label">属性</th>
                <th class="info-value">值</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="item in cpuData" :key="item.attribute">
                <td class="info-label">{{ item.attribute }}</td>
                <td class="info-value">{{ item.value }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 内存信息 -->
        <div class="info-box memory-info">
          <div class="info-header">
            <img src="@/assets/images/img/memory.png"> 内存
            <span class="status-tag" :class="monitorStatus.memory.level">{{ monitorStatus.memory.text }}</span>
            <span class="status-tag" :class="monitorStatus.jvm.level">JVM {{ monitorStatus.jvm.text }}</span>
          </div>
          <div class="info-content">
            <table>
              <thead>
              <tr>
                <th class="info-label">属性</th>
                <th class="info-value">内存</th>
                <th class="info-value jvm-value">JVM</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="item in memoryData" :key="item.attribute">
                <td class="info-label">{{ item.attribute }}</td>
                <td class="info-value">{{ item.memoryValue }}</td>
                <td class="info-value jvm-value">{{ item.jvmValue }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 服务器信息和Java虚拟机信息 -->
      <div class="info-container server-jvm-container">
        <!-- 服务器信息 -->
        <div class="info-box server-info">
          <div class="info-header">
            <img src="@/assets/images/img/service.png"> 服务器信息
          </div>
          <div class="info-content">
            <table>
              <tbody>
              <tr>
                <td class="info-label">服务器名称</td>
                <td class="info-value">{{ serverData.value[0] }}</td>
                <td class="info-label">操作系统</td>
                <td class="info-value">{{ serverData.value[2] }}</td>
              </tr>
              <tr>
                <td class="info-label">服务器IP</td>
                <td class="info-value">{{ serverData.value[1] }}</td>
                <td class="info-label">系统架构</td>
                <td class="info-value">{{ serverData.value[3] }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Java 虚拟机信息 -->
        <div class="info-box jvm-info">
          <div class="info-header">
            <img src="@/assets/images/img/java.png"> Java虚拟机信息
          </div>
          <div class="info-content">
            <table>
              <tbody>
              <tr v-for="item in jvmData" :key="item.attribute">
                <td class="info-label">{{ item.attribute }}</td>
                <td class="info-value">{{ item.value }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 磁盘信息 -->
      <div class="info-box disk-info">
        <div class="info-header">
          <img src="@/assets/images/img/service.png"> 磁盘信息
        </div>
        <div class="info-content">
          <table class="disk-table">
            <thead>
            <tr>
              <th>盘符路径</th>
              <th>文件系统</th>
              <th>盘符类型</th>
              <th>总大小</th>
              <th>已用</th>
              <th>剩余</th>
              <th>使用率</th>
              <th>状态</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in diskData" :key="`${item.dirName}-${item.typeName}`">
              <td>{{ item.dirName }}</td>
              <td>{{ item.typeName }}</td>
              <td>{{ item.sysTypeName }}</td>
              <td>{{ item.total }}</td>
              <td>{{ item.used }}</td>
              <td>{{ item.free }}</td>
              <td class="usage-cell">
                <span>{{ formatUsage(item.usage) }}%</span>
                <div class="usage-track">
                  <div
                    class="usage-bar"
                    :class="usageLevel(item.usage)"
                    :style="{ width: `${Math.min(Number(item.usage) || 0, 100)}%` }"
                  />
                </div>
              </td>
              <td>
                <span class="status-tag inline" :class="usageStatus(item.usage).level">
                  {{ usageStatus(item.usage).text }}
                </span>
              </td>
            </tr>
            <tr v-if="diskData.length === 0">
              <td class="empty-cell" colspan="8">暂无磁盘信息</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import httpInstance from '@/utils/http';

// 初始化数据
const cpuData = ref([
  { attribute: '核心数', value: '' },
  { attribute: '用户使用率', value: '' },
  { attribute: '系统使用率', value: '' },
  { attribute: '当前空闲率', value: '' }
]);

const memoryData = ref([
  { attribute: '总内存', memoryValue: '', jvmValue: '' },
  { attribute: '已用内存', memoryValue: '', jvmValue: '' },
  { attribute: '剩余内存', memoryValue: '', jvmValue: '' },
  { attribute: '使用率', memoryValue: '', jvmValue: '' }
]);

const serverData = ref({
  value: ['', '', '', '']
});

const jvmData = ref([
  { attribute: 'Java名称', value: '' },
  { attribute: '启动时间', value: '' },
  { attribute: '安装路径', value: '' },
  { attribute: '项目路径', value: '' },
  { attribute: '运行参数', value: '' }
]);

const diskData = ref([]);
const monitorStatus = ref({
  cpu: { level: 'normal', text: '正常' },
  memory: { level: 'normal', text: '正常' },
  jvm: { level: 'normal', text: '正常' }
});

const formatUsage = (usage) => {
  const value = Number(usage);
  return Number.isFinite(value) ? value.toFixed(2) : '0.00';
};

const usageLevel = (usage) => {
  return usageStatus(usage).level;
};

const usageStatus = (usage) => {
  const value = Number(usage);
  if (value >= 90) {
    return { level: 'danger', text: '危险' };
  }
  if (value >= 80) {
    return { level: 'warning', text: '警告' };
  }
  return { level: 'normal', text: '正常' };
};

// 获取服务器监控信息
const fetchServerInfo = async () => {
  try {
    const response = await httpInstance.get('/api/v1/monitor/serverMonitor');
    const data = response?.data;
    if (!data) {
      throw new Error('服务器监控数据为空');
    }

    // CPU 数据
    const cpuUsage = Number((100 - Number(data.cpu.free)).toFixed(2));
    cpuData.value = [
      { attribute: '核心数', value: data.cpu.cpuNum },
      { attribute: '用户使用率', value: `${data.cpu.used}%` },
      { attribute: '系统使用率', value: `${data.cpu.sys}%` },
      { attribute: '当前空闲率', value: `${data.cpu.free}%` }
    ];

    // 内存数据
    memoryData.value = [
      { attribute: '总内存', memoryValue: `${data.mem.total}G`, jvmValue: `${(data.jvm.max / 1024).toFixed(2)}G` },
      { attribute: '已用内存', memoryValue: `${data.mem.used}G`, jvmValue: `${(data.jvm.used / 1024).toFixed(2)}G` },
      { attribute: '剩余内存', memoryValue: `${data.mem.free}G`, jvmValue: `${(data.jvm.free / 1024).toFixed(2)}G` },
      { attribute: '使用率', memoryValue: `${data.mem.usage}%`, jvmValue: `${data.jvm.usage}%` }
    ];

    // 服务器信息
    serverData.value.value = [
      data.sys.computerName,
      data.sys.computerIp,
      data.sys.osName,
      data.sys.osArch
    ];

    // Java 虚拟机信息
    jvmData.value = [
      { attribute: 'Java名称', value: data.jvm.name },
      { attribute: '启动时间', value: data.jvm.startTime },
      { attribute: '安装路径', value: data.jvm.home },
      { attribute: '项目路径', value: data.sys.userDir },
      { attribute: '运行参数', value: data.jvm.inputArgs }
    ];

    diskData.value = Array.isArray(data.sysFiles) ? data.sysFiles : [];
    monitorStatus.value = {
      cpu: usageStatus(cpuUsage),
      memory: usageStatus(data.mem.usage),
      jvm: usageStatus(data.jvm.usage)
    };
  } catch (error) {
    console.error('Failed to fetch server info:', error);
  }
};

onMounted(() => {
  fetchServerInfo();
});
</script>

<style scoped>
.outer-service-control {
  background-color: #ffffff;
  padding-top: 8px;
  padding-left: 200px;
  padding-right: 200px;
  margin-top: 0;
  padding-bottom: 30px; /* 在外层容器底部添加内边距 */
}

.service-control {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-container {
  display: flex;
  gap: 20px;
}

.cpu-info{
  flex: 3;
}
.memory-info{
  flex: 4;
}
.server-jvm-container {
  flex-direction: column;
}

.server-info,
.jvm-info {
  width: 100%;
}

.info-box {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
  color: #303133;
  transition: .3s;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); /* 添加阴影效果 */
}

.info-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  box-sizing: border-box;
  font-size: 16px;
  font-weight: 500;
  color: #707070;
}
.info-header img {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  margin-left: 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.status-tag.inline {
  margin-left: 0;
}

.status-tag.normal {
  color: #529b2e;
  background-color: #f0f9eb;
  border: 1px solid #d1edc4;
}

.status-tag.warning {
  color: #b88230;
  background-color: #fdf6ec;
  border: 1px solid #f3d19e;
}

.status-tag.danger {
  color: #c45656;
  background-color: #fef0f0;
  border: 1px solid #fab6b6;
}

.info-content {
  padding: 16px 20px;
  box-sizing: border-box;
}

.info-content table {
  width: 100%;
  border-collapse: collapse;
}

.info-content table th,
.info-content table td {
  padding: 12px; /* 增加内边距 */
  border-bottom: 1px solid #ebeef5;
  text-align: left; /* 设置文本左对齐 */
}

.info-content table th.info-label,
.info-content table td.info-label {
  width: 100px; /* 减小宽度以左移“属性”列 */
  text-align: left; /* “属性”列左对齐 */
  color: #909399;
}

.info-content table th.info-value,
.info-content table td.info-value {
  flex: 1;
  color: #909399;
  padding-left: 150px;
}

.info-content table th.jvm-value,
.info-content table td.jvm-value {
  padding-left: 45px;
}

.jvm-value {
  margin-left: 20px;
  color: #67C23A;
}

.info-row:last-child {
  margin-bottom: 0;
}

.disk-info {
  width: 100%;
}

.disk-table th,
.disk-table td {
  color: #909399;
  white-space: nowrap;
}

.usage-cell {
  min-width: 150px;
}

.usage-track {
  height: 6px;
  margin-top: 8px;
  overflow: hidden;
  background-color: #ebeef5;
  border-radius: 999px;
}

.usage-bar {
  height: 100%;
  border-radius: 999px;
}

.usage-bar.normal {
  background-color: #67C23A;
}

.usage-bar.warning {
  background-color: #E6A23C;
}

.usage-bar.danger {
  background-color: #F56C6C;
}

.empty-cell {
  text-align: center;
  color: #c0c4cc;
}
</style>

