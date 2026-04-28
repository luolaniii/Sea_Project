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
          <span class="chart-type">{{ chartTypeLabel }}</span>
          <span v-if="sourceLabel" class="chart-source" title="此图表的数据来自该站点">
            <Icon name="signal" :size="13" /> 数据源：{{ sourceLabel }}
          </span>
          <span v-if="timeRangeLabel" class="chart-timerange">
            <Icon name="refresh" :size="13" /> {{ timeRangeLabel }}
          </span>
          <span class="chart-count">共 {{ chartData.length }} 条观测</span>
        </div>
        <div v-if="stationCtx" class="chart-links">
          <span class="chart-source chart-station-type">
            <Icon name="database" :size="12" />
            {{ stationCtx.stationTypeDesc }}
          </span>
          <button class="link-btn" :disabled="!stationCtx.sceneId" @click="goToScene">
            <Icon name="scene" :size="12" /> 3D 场景
          </button>
          <button class="link-btn" @click="goToAi">
            <Icon name="brain" :size="12" /> AI 分析
          </button>
          <button class="link-btn link-btn-ghost" :disabled="!stationCtx.officialUrl" @click="openOfficial">
            <Icon name="external" :size="12" /> 站点官网
          </button>
          <button class="link-btn link-btn-ghost" :disabled="!stationCtx.historyUrl" @click="openHistory">
            <Icon name="database" :size="12" /> 历史数据
          </button>
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
      <section v-if="volatileSegments.length" class="volatility-panel">
        <div class="volatility-head">
          <div>
            <strong>剧烈变化时间段</strong>
            <span>基于当前回放时间点最近 24 小时，按相邻观测差值识别</span>
          </div>
        </div>
        <div class="volatility-list">
          <div v-for="seg in volatileSegments" :key="`${seg.typeCode}-${seg.startTime}-${seg.endTime}`" :class="['volatility-item', seg.level]">
            <span class="metric">{{ seg.typeLabel }}</span>
            <span class="time">{{ seg.startTime }} ~ {{ seg.endTime }}</span>
            <span class="delta">变化 {{ seg.delta }}</span>
            <span class="values">{{ seg.fromValue }} → {{ seg.toValue }}</span>
          </div>
        </div>
      </section>
      <div class="chart-content">
        <div class="chart-viewer-wrap">
          <Chart2DViewer
            :key="chartRenderKey"
            :echarts-config="chart.echartsConfig"
            :data-query-config="chart.dataQueryConfig"
            :chart-type="chartTypeCode"
            :data="chartDataReplay"
          />
          <div v-if="chartData.length === 0" class="chart-empty-tip">
            当前图表暂无可用观测数据，已自动尝试放宽查询条件
          </div>
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
import { buildVolatileSegmentsFromObservations } from '@/utils/volatile-segments';

const formatMsForApi = (ms: number): string => {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const toApiTimeString = (input?: string): string | undefined => {
  if (!input) return undefined;
  const t = String(input).trim();
  if (!t) return undefined;
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(t)) return t;
  const parsed = Date.parse(t);
  if (Number.isNaN(parsed)) return undefined;
  return formatMsForApi(parsed);
};

const extractPageList = (result: any): any[] => {
  if (Array.isArray(result)) return result;
  if (Array.isArray(result?.list)) return result.list;
  if (Array.isArray(result?.records)) return result.records;
  if (Array.isArray(result?.rows)) return result.rows;
  if (Array.isArray(result?.data?.list)) return result.data.list;
  if (Array.isArray(result?.data)) return result.data;
  return [];
};

const route = useRoute();
const router = useRouter();
const chart = ref<any>(null);
const chartData = ref<any[]>([]);
const loading = ref(true);
const replayTimeMs = ref(0);
const mapStations = ref<any[]>([]);
const mapStationsLoaded = ref(false);

const normalizeChartType = (raw?: unknown): 'LINE' | 'BAR' | 'SCATTER' | 'HEATMAP' | '3D_SURFACE' => {
  const source = String(raw || '').trim();
  const upper = source.toUpperCase();
  const map: Record<string, 'LINE' | 'BAR' | 'SCATTER' | 'HEATMAP' | '3D_SURFACE'> = {
    LINE: 'LINE',
    折线图: 'LINE',
    BAR: 'BAR',
    柱状图: 'BAR',
    SCATTER: 'SCATTER',
    散点图: 'SCATTER',
    HEATMAP: 'HEATMAP',
    热力图: 'HEATMAP',
    '3D_SURFACE': '3D_SURFACE',
    '3D表面图': '3D_SURFACE',
    '3D 表面图': '3D_SURFACE',
  };
  return map[source] || map[upper] || 'LINE';
};

const chartTypeCode = computed(() => {
  if (!chart.value) return 'LINE';
  return normalizeChartType(chart.value.chartTypeCode || chart.value.chartType || chart.value.chartTypeDesc);
});

const chartTypeLabel = computed(() => {
  const map: Record<string, string> = {
    LINE: '折线图',
    BAR: '柱状图',
    SCATTER: '散点图',
    HEATMAP: '热力图',
    '3D_SURFACE': '3D 表面图',
  };
  return map[chartTypeCode.value] || '图表';
});

const chartRenderKey = computed(() => `${chartIdFromRoute()}-${chartTypeCode.value}-${chartDataReplay.value.length}`);
const chartDataSourceId = computed<string>(() => {
  const cfg = parseDataQueryConfigJson(chart.value?.dataQueryConfig);
  const id = cfg?.dataSourceId;
  if (id == null || String(id).trim() === '') return '';
  return String(id).trim();
});

const stationCtx = computed(() => {
  if (!chartDataSourceId.value) return null;
  const hit = mapStations.value.find((s: any) => String(s?.id) === chartDataSourceId.value);
  if (!hit) return null;
  const scenes = Array.isArray(hit.scenes) ? hit.scenes : [];
  return {
    dataSourceId: String(hit.id),
    stationTypeDesc: hit.stationTypeDesc || hit.sourceType || '未分类站点',
    sceneId: scenes.length > 0 ? scenes[0].id : null,
    officialUrl: String(hit.officialUrl || hit.apiUrl || '').trim(),
    historyUrl: String(hit.historyUrl || '').trim(),
  };
});

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

const chartDataRecent24h = computed(() => {
  if (!replayRange.value.valid) return chartData.value;
  const endMs = Math.min(Math.max(replayTimeMs.value, replayRange.value.min), replayRange.value.max);
  const startMs = endMs - 24 * 60 * 60 * 1000;
  return (chartData.value || []).filter((item: any) => {
    const t = parseObservationTimeToMs(item.observationTime);
    return !Number.isNaN(t) && t >= startMs && t <= endMs;
  });
});

const volatileSegments = computed(() => buildVolatileSegmentsFromObservations(chartDataRecent24h.value, { maxSegments: 8, maxPerType: 2 }));

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

const loadMapStations = async () => {
  if (mapStationsLoaded.value) return;
  try {
    mapStations.value = await userApi.getMapStations();
    mapStationsLoaded.value = true;
  } catch (e) {
    console.error('加载站点映射失败:', e);
    mapStations.value = [];
  }
};

const goToScene = () => {
  if (!stationCtx.value?.sceneId) return;
  router.push(`/user/scene/${encodeURIComponent(String(stationCtx.value.sceneId))}`);
};

const goToAi = () => {
  if (!stationCtx.value?.dataSourceId) return;
  router.push({ path: '/user/ocean-analysis', query: { dataSourceId: stationCtx.value.dataSourceId } });
};

const openOfficial = () => {
  const url = stationCtx.value?.officialUrl;
  if (!url) return;
  window.open(url, '_blank');
};

const openHistory = () => {
  const url = stationCtx.value?.historyUrl;
  if (!url) return;
  window.open(url, '_blank');
};

// 加载图表数据
const loadChartData = async (chartId: string, dataQueryConfig?: string) => {
  try {
    const queryConfig = parseDataQueryConfigJson(dataQueryConfig);
    const queryReq: any = {
      pageNum: 1,
      pageSize: 5000, // 取更多已导入历史观测，供历史回放使用
    };

    // 如果有配置，使用配置中的参数
    if (queryConfig) {
      if (queryConfig.dataSourceId) queryReq.dataSourceId = queryConfig.dataSourceId;
      if (queryConfig.dataTypeId) queryReq.dataTypeId = queryConfig.dataTypeId;
      if (queryConfig.typeCodes) queryReq.typeCodes = queryConfig.typeCodes;
      if (queryConfig.startTime) queryReq.startTime = toApiTimeString(queryConfig.startTime);
      if (queryConfig.endTime) queryReq.endTime = toApiTimeString(queryConfig.endTime);
      if (queryConfig.minLongitude !== undefined) queryReq.minLongitude = queryConfig.minLongitude;
      if (queryConfig.maxLongitude !== undefined) queryReq.maxLongitude = queryConfig.maxLongitude;
      if (queryConfig.minLatitude !== undefined) queryReq.minLatitude = queryConfig.minLatitude;
      if (queryConfig.maxLatitude !== undefined) queryReq.maxLatitude = queryConfig.maxLatitude;
      if (queryConfig.pageSize) queryReq.pageSize = queryConfig.pageSize;

      // 支持 config 中以 time.mode/defaultHours 形式配置相对时间范围
      const rel = resolveRelativeTimeRange(queryConfig);
      if (!queryReq.startTime && rel.startTime) queryReq.startTime = toApiTimeString(rel.startTime);
      if (!queryReq.endTime && rel.endTime) queryReq.endTime = toApiTimeString(rel.endTime);
    }

    const first = await userApi.getChartData(chartId, queryReq);
    let list = extractPageList(first);
    if (list.length > 0) return list;

    // 首次为空时自动放宽条件重试，避免图表配置时间窗过窄导致“空白图表”
    const fallbackReq: any = {
      pageNum: 1,
      pageSize: queryReq.pageSize || 1000,
    };

    const second = await userApi.getChartData(chartId, fallbackReq);
    list = extractPageList(second);
    return list;
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
      const rawValue = item.dataValue != null ? item.dataValue : item.value;
      const value = rawValue != null ? Number(rawValue) : rawValue;
      const dep = item.depth != null ? Number(item.depth) : item.depth;
      const obsTime = item.observationTime ?? item.time ?? item.timestamp;
      return {
        ...item,
        observationTime: obsTime,
        longitude: isNaN(lon) ? item.longitude : lon,
        latitude: isNaN(lat) ? item.latitude : lat,
        dataValue: isNaN(value) ? rawValue : value,
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
  loadMapStations();
  loadChart();
});

watch(
  () => route.params.id,
  () => {
    loadChart();
  }
);
</script>

<style scoped lang="less">
.user-chart-view {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at 100% -10%, rgba(125, 211, 252, 0.22), transparent 42%),
    linear-gradient(180deg, #f4f9ff 0%, #f6f8fb 100%);
  position: relative;
}

.loading,
.error {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
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
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid #dbe8f4;
  box-shadow: 0 8px 24px -18px rgba(2, 132, 199, 0.45);
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
  background: #f8fbff;
  border: 1px solid #dbe8f4;
  border-radius: 8px;
  color: #0369a1;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.btn-back:hover {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0c4a6e;
  transform: translateX(-2px);
}

.back-icon {
  font-size: 18px;
  font-weight: bold;
}

.chart-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
  letter-spacing: 0.3px;
}

.chart-description {
  font-size: 15px;
  color: #475569;
  margin-bottom: 16px;
  line-height: 1.6;
  letter-spacing: 0.2px;
}

.chart-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #64748b;
  flex-wrap: wrap;
}

.chart-type {
  padding: 6px 14px;
  background: #e0f2fe;
  border-radius: 12px;
  font-weight: 600;
  border: 1px solid #bae6fd;
  color: #0369a1;
}

.chart-source,
.chart-timerange,
.chart-count {
  padding: 6px 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbe8f4;
  color: #334155;
  font-size: 12px;
  font-weight: 500;
}
.chart-timerange {
  background: #fef9e7;
  border-color: #fde68a;
  color: #92400e;
}
.chart-count {
  background: #ecfeff;
  border-color: #a5f3fc;
  color: #0c4a6e;
}

.chart-links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.chart-station-type {
  background: #ecfeff;
  border-color: #a5f3fc;
  color: #0c4a6e;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #dbe8f4;
  border-radius: 9px;
  background: #f8fbff;
  color: #0f172a;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: #e0f2fe;
    border-color: #7dd3fc;
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.link-btn-ghost {
  background: #fff;
  color: #475569;
}

.chart-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 32px;
  padding-bottom: 24px;
  min-height: 600px;
  background: transparent;
  position: relative;
}

.chart-viewer-wrap {
  flex: 1;
  min-height: 400px;
  position: relative;
  background: #ffffff;
  border: 1px solid #dbe8f4;
  border-radius: 14px;
  box-shadow: 0 16px 38px -24px rgba(2, 132, 199, 0.4);
  padding: 8px;
}

.chart-empty-tip {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fff7e6;
  border: 1px solid #fde68a;
  color: #b45309;
  font-size: 12px;
  pointer-events: none;
}

.chart-replay-bar-host {
  flex-shrink: 0;
  position: relative;
  z-index: 25;
  padding: 0 32px;
  border-bottom: 1px solid #dbe8f4;
  background: rgba(255, 255, 255, 0.7);
}

.replay-unavailable-hint {
  flex-shrink: 0;
  margin: 0;
  padding: 10px 32px;
  font-size: 12px;
  color: #b45309;
  background: #fff7e6;
  border-bottom: 1px solid #fde68a;
}

.volatility-panel {
  margin: 16px 32px 0;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid #dbe8f4;
  border-radius: 14px;
  box-shadow: 0 14px 34px -26px rgba(2, 132, 199, 0.45);
}

.volatility-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;

  strong {
    display: block;
    color: #0f172a;
    font-size: 14px;
    margin-bottom: 2px;
  }

  span {
    color: #475569;
    font-size: 12px;
  }
}

.volatility-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.volatility-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbe8f4;
  color: #334155;
  font-size: 12px;

  &.high {
    background: #fff7ed;
    border-color: #fed7aa;
  }

  .metric {
    grid-row: span 2;
    align-self: center;
    color: #0369a1;
    font-weight: 700;
  }

  .time {
    color: #0f172a;
    font-weight: 600;
  }

  .delta,
  .values {
    color: #475569;
  }
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

  .volatility-panel {
    margin: 14px 16px 0;
  }
}
</style>

