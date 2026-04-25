<template>
  <div class="user-chart-view">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="chart" class="chart-container">
      <div class="chart-header">
        <div class="chart-header-top">
          <button class="btn-back" @click="handleBack">
            <span class="back-icon">←</span>
            <span>返回</span>
          </button>
        </div>
        <h1 class="chart-title">{{ chart.chartName }}</h1>
        <p class="chart-description">{{ chart.description || '暂无描述' }}</p>
        <div class="chart-meta">
          <span class="chart-type">{{ chart.chartType }}</span>
          <span v-if="sourceLabel" class="chart-source" title="此图表的数据来自该站点">
            <Icon name="signal" :size="13" /> 数据源：{{ sourceLabel }}
          </span>
          <span v-if="timeRangeLabel" class="chart-timerange">
            <Icon name="refresh" :size="13" /> {{ timeRangeLabel }}
          </span>
          <span class="chart-count">共 {{ chartData.length }} 条观测</span>
        </div>
      </div>
      <div v-if="replayRange.valid" class="chart-replay-bar-host">
        <TimeReplayBar
          v-model="replayTimeMs"
          :min-ms="replayRange.min"
          :max-ms="replayRange.max"
        />
      </div>
      <p v-else-if="chartData.length > 0" class="replay-unavailable-hint">
        当前数据无法解析观测时间，历史回放不可用
      </p>
      <div class="chart-content">
        <div class="chart-viewer-wrap">
          <Chart2DViewer
            :echarts-config="chart.echartsConfig"
            :data-query-config="chart.dataQueryConfig"
            :chart-type="chart.chartType"
            :data="chartDataReplay"
          />
        </div>
      </div>
    </div>
    <div v-else class="error">图表不存在</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Chart2DViewer from '@/components/Chart2DViewer.vue';
import TimeReplayBar from '@/components/TimeReplayBar.vue';
import Icon from '@/components/Icon.vue';
import { userApi } from '@/utils/api-user';
import { routeParamId } from '@/utils/path-id';
import { parseDataQueryConfigJson, resolveRelativeTimeRange } from '@/utils/data-query-config';
import { parseObservationTimeToMs } from '@/utils/observation-time';

const formatMsForApi = (ms: number): string => {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const route = useRoute();
const router = useRouter();
const chart = ref<any>(null);
const chartData = ref<any[]>([]);
const loading = ref(true);
const replayTimeMs = ref(0);

// 数据源标签与时间范围：从观测数据自动聚合，让用户看出数据来源
const sourceLabel = computed<string>(() => {
  const list = chartData.value || [];
  const names = new Set<string>();
  const stations = new Set<string>();
  list.forEach((d: any) => {
    if (d?.dataSourceName) names.add(String(d.dataSourceName));
    else if (d?.stationId) stations.add(String(d.stationId));
  });
  if (names.size > 0) return Array.from(names).join('，');
  if (stations.size > 0) return Array.from(stations).map(s => `站点 ${s}`).join('，');
  return '';
});

const timeRangeLabel = computed<string>(() => {
  const list = chartData.value || [];
  let min = Infinity;
  let max = -Infinity;
  for (const d of list) {
    const t = parseObservationTimeToMs(d?.observationTime);
    if (!Number.isNaN(t)) {
      if (t < min) min = t;
      if (t > max) max = t;
    }
  }
  if (!Number.isFinite(min) || min > max) return '';
  const fmt = (ms: number) => {
    const d = new Date(ms);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
  };
  return `${fmt(min)} ~ ${fmt(max)}`;
});

const replayRange = computed(() => {
  const list = chartData.value || [];
  let min = Infinity;
  let max = -Infinity;
  for (const item of list) {
    const t = parseObservationTimeToMs(item.observationTime);
    if (!Number.isNaN(t)) {
      min = Math.min(min, t);
      max = Math.max(max, t);
    }
  }
  if (!Number.isFinite(min) || min > max) {
    return { valid: false as const, min: 0, max: 0 };
  }
  return { valid: true as const, min, max };
});

const replayEndTimeStr = computed(() => {
  if (!replayRange.value.valid) return '';
  const ms = Math.min(Math.max(replayTimeMs.value, replayRange.value.min), replayRange.value.max);
  return formatMsForApi(ms);
});

const chartDataReplay = computed(() => {
  if (!replayRange.value.valid) return chartData.value;
  const endMs = parseObservationTimeToMs(replayEndTimeStr.value);
  if (Number.isNaN(endMs)) return chartData.value;
  return (chartData.value || []).filter((item: any) => {
    const t = parseObservationTimeToMs(item.observationTime);
    if (Number.isNaN(t)) return false;
    return t <= endMs;
  });
});

watch(
  () => replayRange.value,
  (r) => {
    if (r.valid && (replayTimeMs.value < r.min || replayTimeMs.value > r.max || replayTimeMs.value === 0)) {
      replayTimeMs.value = r.max;
    }
  },
  { immediate: true }
);

const handleBack = () => {
  router.push('/user/chart-gallery');
};

// 加载图表数据
const loadChartData = async (chartId: string, dataQueryConfig?: string) => {
  try {
    const queryConfig = parseDataQueryConfigJson(dataQueryConfig);
    const queryReq: any = {
      pageNum: 1,
      pageSize: 1000, // 默认查询1000条数据
    };

    // 如果有配置，使用配置中的参数
    if (queryConfig) {
      if (queryConfig.dataSourceId) queryReq.dataSourceId = queryConfig.dataSourceId;
      if (queryConfig.dataTypeId) queryReq.dataTypeId = queryConfig.dataTypeId;
      if (queryConfig.typeCodes) queryReq.typeCodes = queryConfig.typeCodes;
      if (queryConfig.startTime) queryReq.startTime = queryConfig.startTime;
      if (queryConfig.endTime) queryReq.endTime = queryConfig.endTime;
      if (queryConfig.minLongitude !== undefined) queryReq.minLongitude = queryConfig.minLongitude;
      if (queryConfig.maxLongitude !== undefined) queryReq.maxLongitude = queryConfig.maxLongitude;
      if (queryConfig.minLatitude !== undefined) queryReq.minLatitude = queryConfig.minLatitude;
      if (queryConfig.maxLatitude !== undefined) queryReq.maxLatitude = queryConfig.maxLatitude;
      if (queryConfig.pageSize) queryReq.pageSize = queryConfig.pageSize;

      // 支持 config 中以 time.mode/defaultHours 形式配置相对时间范围
      const rel = resolveRelativeTimeRange(queryConfig);
      if (!queryReq.startTime && rel.startTime) queryReq.startTime = rel.startTime;
      if (!queryReq.endTime && rel.endTime) queryReq.endTime = rel.endTime;
    }

    const result = await userApi.getChartData(chartId, queryReq);
    return result.list || [];
  } catch (error) {
    console.error('加载图表数据失败:', error);
    return [];
  }
};

const chartIdFromRoute = (): string => routeParamId(route.params.id);

const loadChart = async () => {
  const chartId = chartIdFromRoute();
  if (!chartId) {
    loading.value = false;
    return;
  }

  try {
    loading.value = true;
    const data = await userApi.getChartById(chartId);
    chart.value = data;

    // 加载图表数据
    const dataList = await loadChartData(chartId, data.dataQueryConfig);
    // 保留后端 ObservationDataVO 原始结构（含 dataTypeCode），供 Chart2DViewer 按 echarts_config.encode 自动映射
    chartData.value = (dataList || []).map((item: any) => {
      const lon = item.longitude != null ? Number(item.longitude) : item.longitude;
      const lat = item.latitude != null ? Number(item.latitude) : item.latitude;
      const value = item.dataValue != null ? Number(item.dataValue) : item.dataValue;
      const dep = item.depth != null ? Number(item.depth) : item.depth;
      return {
        ...item,
        longitude: isNaN(lon) ? item.longitude : lon,
        latitude: isNaN(lat) ? item.latitude : lat,
        dataValue: isNaN(value) ? item.dataValue : value,
        depth: isNaN(dep) ? item.depth : dep,
      };
    });
  } catch (error) {
    console.error('加载图表失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadChart();
});
</script>

<style scoped lang="less">
.user-chart-view {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1f3a 100%);
  position: relative;
}

.loading,
.error {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;
  min-height: 400px;
}

.chart-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.chart-header {
  padding: 28px 32px;
  background: rgba(15, 20, 45, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 10;
}

.chart-header-top {
  margin-bottom: 16px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.4);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.btn-back:hover {
  background: rgba(102, 126, 234, 0.3);
  border-color: rgba(102, 126, 234, 0.6);
  color: #fff;
  transform: translateX(-2px);
}

.back-icon {
  font-size: 18px;
  font-weight: bold;
}

.chart-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.3px;
}

.chart-description {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 16px;
  line-height: 1.6;
  letter-spacing: 0.2px;
}

.chart-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.chart-type {
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3));
  border-radius: 12px;
  font-weight: 500;
  border: 1px solid rgba(102, 126, 234, 0.4);
}

.chart-source,
.chart-timerange,
.chart-count {
  padding: 6px 12px;
  border-radius: 10px;
  background: rgba(92, 246, 196, 0.08);
  border: 1px solid rgba(92, 246, 196, 0.3);
  color: rgba(200, 255, 230, 0.9);
  font-size: 12px;
  font-weight: 500;
}
.chart-timerange {
  background: rgba(255, 203, 94, 0.08);
  border-color: rgba(255, 203, 94, 0.3);
  color: rgba(255, 230, 180, 0.95);
}
.chart-count {
  background: rgba(102, 126, 234, 0.12);
  border-color: rgba(102, 126, 234, 0.3);
  color: rgba(210, 220, 255, 0.9);
}

.chart-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 32px;
  padding-bottom: 24px;
  min-height: 600px;
  background: rgba(10, 14, 39, 0.3);
  position: relative;
}

.chart-viewer-wrap {
  flex: 1;
  min-height: 400px;
  position: relative;
}

.chart-replay-bar-host {
  flex-shrink: 0;
  position: relative;
  z-index: 25;
  padding: 0 32px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
}

.replay-unavailable-hint {
  flex-shrink: 0;
  margin: 0;
  padding: 10px 32px;
  font-size: 12px;
  color: rgba(255, 200, 120, 0.85);
  background: rgba(15, 20, 45, 0.95);
  border-bottom: 1px solid rgba(255, 180, 80, 0.25);
}

// 响应式设计
@media (max-width: 768px) {
  .chart-header {
    padding: 20px 24px;
  }

  .chart-title {
    font-size: 22px;
  }

  .chart-description {
    font-size: 14px;
  }

  .chart-content {
    padding: 24px;
    min-height: 400px;
  }

  .chart-replay-bar-host {
    padding: 0 16px;
  }
}
</style>

