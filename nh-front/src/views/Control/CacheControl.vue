<template>
  <div class="outer-service-control">
    <div class="service-control">
      <!-- Redis 基本信息 -->
      <div class="info-container">
        <div class="info-box redis-info">
          <div class="info-header">
            <img src="@/assets/images/img/service.png"> Redis基本信息
          </div>
          <div class="info-content">
            <table>
              <tbody>
              <tr>
                <td class="info-label">Redis版本</td>
                <td class="info-value">{{ informationFirst[0].value }}</td>
                <td class="info-label">运行模式</td>
                <td class="info-value">{{ informationSecond[0].value === "standalone" ? "单机" : "集群" }}</td>
                <td class="info-label">端口</td>
                <td class="info-value">{{ informationThird[0].value }}</td>
              </tr>
              <tr>
                <td class="info-label">运行时间</td>
                <td class="info-value">{{ informationFirst[1].value }}</td>
                <td class="info-label">使用内存</td>
                <td class="info-value">{{ informationSecond[1].value }}</td>
                <td class="info-label">使用CPU</td>
                <td class="info-value">{{ informationThird[1].value }}</td>
              </tr>
              <tr>
                <td class="info-label">AOF是否开启</td>
                <td class="info-value">{{ informationFirst[2].value === "1" ? "是" : "否" }}</td>
                <td class="info-label">RDB是否成功</td>
                <td class="info-value">{{ informationSecond[2].value === "ok" ? "是" : "否" }}</td>
                <td class="info-label">Key数量</td>
                <td class="info-value">{{ getDbKeys(informationThird[2].value) }}</td>
              </tr>
              <tr>
                <td class="info-label">客户端数</td>
                <td class="info-value">{{ informationForth[0].value }}</td>
                <td class="info-label">内存配置</td>
                <td class="info-value">{{ informationForth[1].value }}</td>
                <td class="info-label">网络入口/出口</td>
                <td class="info-value">{{ informationForth[2].value }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 命令统计 -->
      <div class="info-container">
        <div class="info-box command-stats">
          <div class="info-header">
            <img src="@/assets/images/img/command.png"> 命令统计
          </div>
          <div ref="commandstats" style="height: 420px; width: 100%; min-width: 300px;"></div> <!-- 确保 ref 正确绑定 -->
        </div>

        <!-- 内存信息 -->
        <div class="info-box memory-info">
          <div class="info-header">
            <img src="@/assets/images/img/odometer.png"> 内存信息
          </div>
          <div ref="usedmemory" style="height: 420px; width: 100%;"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import httpInstance from '@/utils/http';
import * as echarts from 'echarts'; // 引入 ECharts

// 初始化数据
const informationFirst = ref([
  { attribute: 'Redis版本', value: '' },
  { attribute: '运行时间(天)', value: '' },
  { attribute: 'AOF是否开启', value: '' }
]);

const informationSecond = ref([
  { attribute: '运行模式', value: ''},
  { attribute: '使用内存', value: '' },
  { attribute: 'RDB是否成功', value: '' }
]);

const informationThird = ref([
  { attribute: '端口', value: '' },
  { attribute: '使用CPU', value: '' },
  { attribute: 'Key数量', value: '' }
]);

const informationForth = ref([
  { attribute: '客户端数', value: '' },
  { attribute: '内存配置', value: '' },
  { attribute: '网络入口/出口', value: '' }
]);

const commandStats = ref([]);
const commandstats = ref(null); // 定义 ref
const usedmemory = ref(null); // 定义 ref for usedmemory
const memoryUsage = ref({ value: 0, unit: '' }); // 初始化内存使用值

// 从 db0 字符串中提取 keys 值的函数
const getDbKeys = (dbString) => {
  if (!dbString) return 'N/A';
  // 使用正则表达式匹配 keys= 后面的数字
  const match = dbString.match(/keys=(\d+)/);
  return match ? match[1] : 'N/A';
};

const getIntoredisData = async () => {
  try {
    const response = await httpInstance.get('/api/v1/monitor/getRedisInfo');
    const data = response?.data;
    if (!data) {
      throw new Error('缓存监控数据为空');
    }
    informationFirst.value = [
      {attribute: 'Redis版本', value: data.info.redis_version},
      {attribute: '运行时间(天)', value: data.info.uptime_in_days},
      {attribute: 'AOF是否开启', value: data.info.aof_enabled}
    ];
    informationSecond.value = [
      {attribute: '运行模式', value: data.info.redis_mode},
      {attribute: '使用内存', value: data.info.used_memory_human},
      {attribute: 'RDB是否成功', value: data.info.rdb_last_bgsave_status}
    ];

    // 获取 db0 的值
    let db0Value = 'N/A';
    if (data.info.db0) {
      db0Value = data.info.db0;
    }

    informationThird.value = [
      {attribute: '端口', value: data.info.tcp_port},
      {attribute: '使用CPU', value: data.info.used_cpu_user_children},
      {attribute: 'Key数量', value: db0Value}
    ];
    informationForth.value = [
      {attribute: '客户端数', value: data.info.connected_clients},
      {attribute: '内存配置', value: data.info.maxmemory_human},
      {
        attribute: '网络入口/出口',
        value: `${data.info.instantaneous_input_kbps}kbps` + '/' + `${data.info.instantaneous_output_kbps}kbps`
      }
    ];

    // 设置 commandStats 数据
    commandStats.value = data.commandStats || [];

    // 设置内存使用值
    const usedMemoryHuman = data.info.used_memory_human;
    let memoryValue = 0;
    let memoryUnit = '';

    if (usedMemoryHuman) {
      const valueMatch = usedMemoryHuman.match(/^(\d+(\.\d+)?)([KMG])$/);
      if (valueMatch) {
        memoryValue = parseFloat(valueMatch[1]);
        memoryUnit = valueMatch[3];
      }
    }

    memoryUsage.value = {value: memoryValue, unit: memoryUnit};

    // 触发 nextTick 重新初始化图表
    nextTick(() => {
      initCommandStatsChart();
      initMemoryUsageChart(); // 初始化内存使用图表
    });

  } catch (error) {
    console.error('Failed to fetch server info:', error);
  }
};

// 初始化命令统计图表
const initCommandStatsChart = () => {
  if (!commandstats.value) {
    console.error('commandstats ref is not properly bound');
    return;
  }

  const chartInstance = echarts.init(commandstats.value);

  // 配置项
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      show: false // 隐藏图例
    },
    series: [
      {
        name: '命令统计',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        startAngle: 0, // 调整起始角度为 0
        endAngle: 360, // 调整结束角度为 360
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'outside',
          formatter: '{b}', // 显示标签名称
          fontSize: 12, // 调整字体大小
          color: '#666' // 调整字体颜色
        },
        labelLine: {
          show: true,
          length: 15, // 调整标签线长度
          length2: 10 // 调整标签线第二段长度
        },
        data: commandStats.value.map(item => ({
          value: parseInt(item.value),
          name: item.name.replace('|', '\n')
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };

  // 使用配置项和数据显示图表
  chartInstance.setOption(option);

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => {
    chartInstance.resize();
  });
};

// 初始化内存使用图表 - 马卡龙配色样式
const initMemoryUsageChart = () => {
  if (!usedmemory.value) {
    console.error('usedmemory ref is not properly bound');
    return;
  }

  const chartInstance = echarts.init(usedmemory.value);

  // 配置项 - 马卡龙配色仪表盘样式
  const option = {
    series: [
      {
        type: 'gauge',
        center: ['50%', '60%'],
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        splitNumber: 5,
        itemStyle: {
          color: '#FF9AA2', // 马卡龙粉
          shadowColor: 'rgba(255, 154, 162, 0.3)',
          shadowBlur: 8,
          shadowOffsetX: 2,
          shadowOffsetY: 2
        },
        progress: {
          show: true,
          roundCap: true,
          width: 15
        },
        pointer: {
          icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
          length: '12%',
          width: 12,
          offsetCenter: [0, '-55%'],
          itemStyle: {
            color: '#FF9AA2',
            shadowColor: 'rgba(255, 154, 162, 0.5)',
            shadowBlur: 8,
            shadowOffsetX: 2,
            shadowOffsetY: 2
          }
        },
        axisLine: {
          roundCap: true,
          lineStyle: {
            width: 15,
            color: [
              [0.3, '#B5EAD7'], // 马卡龙绿
              [0.7, '#C7CEEA'], // 马卡龙紫
              [1, '#FF9AA2']    // 马卡龙粉
            ]
          }
        },
        axisTick: {
          splitNumber: 2,
          length: 8,
          lineStyle: {
            width: 2,
            color: '#FFDAC1' // 马卡龙橙
          }
        },
        splitLine: {
          length: 15,
          lineStyle: {
            width: 3,
            color: '#FFDAC1' // 马卡龙橙
          }
        },
        axisLabel: {
          distance: 25,
          color: '#888',
          fontSize: 14,
          fontWeight: 'normal'
        },
        title: {
          show: true,
          offsetCenter: [0, '70%'],
          fontSize: 18,
          fontWeight: 'normal',
          color: '#666'
        },
        detail: {
          valueAnimation: true,
          width: '100%',
          lineHeight: 40,
          borderRadius: 8,
          offsetCenter: [0, '0%'],
          fontSize: 26,
          fontWeight: 'bold',
          formatter: function (value) {
            return '{value|' + memoryUsage.value.value.toFixed(2) + '}{unit|' + memoryUsage.value.unit + '}';
          },
          rich: {
            value: {
              color: '#FF9AA2', // 马卡龙粉
              fontSize: 30,
              fontWeight: 'bold'
            },
            unit: {
              color: '#888',
              fontSize: 18
            }
          }
        },
        data: [
          {
            value: Math.min((memoryUsage.value.value / 100) * 100, 100), // 假设最大值为100
            name: '内存使用'
          }
        ]
      }
    ]
  };

  // 使用配置项和数据显示图表
  chartInstance.setOption(option);

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => {
    chartInstance.resize();
  });
};

onMounted(() => {
  getIntoredisData();
});
</script>

<style scoped>
.outer-service-control {
  background-color: #ffffff;
  padding-top: 8px;
  padding-left: 200px;
  padding-right: 200px;
  margin-top: 0;
  padding-bottom: 30px;
}

.service-control {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-container {
  display: flex;
  gap: 20px;
  width: 100%; /* 确保父容器宽度为 100% */
}

.info-box {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
  color: #303133;
  transition: .3s;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  width: 100%; /* 确保 info-box 宽度为 100% */
  box-sizing: border-box; /* 包含边框和内边距在内 */
}

.info-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  font-size: 16px;
  font-weight: 500;
  color: #707070;
}

.info-header img {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.info-content {
  padding: 20px 30px;
}

.info-content table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.info-content table td {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
}

.info-content table .info-label {
  width: 120px;
  text-align: left;
  color: #909399;
  font-weight: bold;
}

.info-content table .info-value {
  width: 220px;
  color: #909399;
  padding-left: 20px;
}

.info-content table tr:last-child td {
  border-bottom: none;
}

.info-row:last-child {
  margin-bottom: 0;
}
</style>

