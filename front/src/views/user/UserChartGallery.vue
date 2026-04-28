<template>
  <div class="user-chart-gallery">
    <PageHeader title="图表画廊" description="浏览和查看数据可视化图表" />

    <div class="filter-panel card">
      <div class="filter-row">
        <div class="filter-item">
          <label>图表分类</label>
          <select v-model="query.chartType" @change="handleFilterChange">
            <option value="">全部分类</option>
            <option v-for="opt in chartTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>站点关键词</label>
          <input
            v-model="query.stationKeyword"
            type="text"
            placeholder="站点名或站点ID"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label>站点类型</label>
          <select v-model="query.stationType" @change="handleFilterChange">
            <option value="">全部站点类型</option>
            <option v-for="t in stationTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div class="filter-item">
          <label>海域</label>
          <select v-model="query.oceanRegion" @change="handleFilterChange">
            <option value="">全部海域</option>
            <option v-for="o in oceanOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <div class="filter-item">
          <label>数据类型</label>
          <input
            v-model="query.dataTypeCode"
            type="text"
            placeholder="例如 WTMP / WAVE_HEIGHT"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item keyword-item">
          <label>关键词搜索</label>
          <input
            v-model="query.keyword"
            type="text"
            placeholder="请输入图表名称关键词"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item coord-item">
          <label>经度范围</label>
          <div class="coord-range">
            <input v-model.number="query.minLongitude" type="number" placeholder="最小经度" />
            <span>~</span>
            <input v-model.number="query.maxLongitude" type="number" placeholder="最大经度" />
          </div>
        </div>
        <div class="filter-item coord-item">
          <label>纬度范围</label>
          <div class="coord-range">
            <input v-model.number="query.minLatitude" type="number" placeholder="最小纬度" />
            <span>~</span>
            <input v-model.number="query.maxLatitude" type="number" placeholder="最大纬度" />
          </div>
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button class="btn btn-default" @click="clearFilters">重置</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="charts.length === 0" class="text-center">暂无图表</div>
    <div v-else class="chart-grid">
      <div v-for="chart in charts" :key="chart.id" class="chart-card card">
        <div class="chart-header">
          <h3 class="chart-name">{{ chart.chartName }}</h3>
          <span class="chart-type">{{ chartTypeLabel(chart) }}</span>
        </div>
        <div class="chart-preview" :class="`preview-${chartTypeCode(chart).toLowerCase()}`">
          <svg v-if="chartTypeCode(chart) === 'LINE'" class="preview-svg" viewBox="0 0 320 180" aria-hidden="true">
            <polyline points="20,130 75,108 128,120 185,72 248,86 300,44" class="preview-line" />
            <circle cx="75" cy="108" r="4" class="preview-dot" />
            <circle cx="185" cy="72" r="4" class="preview-dot" />
            <circle cx="300" cy="44" r="4" class="preview-dot" />
          </svg>
          <svg v-else-if="chartTypeCode(chart) === 'BAR'" class="preview-svg" viewBox="0 0 320 180" aria-hidden="true">
            <rect x="34" y="86" width="28" height="68" class="preview-bar" />
            <rect x="90" y="52" width="28" height="102" class="preview-bar" />
            <rect x="146" y="98" width="28" height="56" class="preview-bar" />
            <rect x="202" y="34" width="28" height="120" class="preview-bar" />
            <rect x="258" y="70" width="28" height="84" class="preview-bar" />
          </svg>
          <svg v-else-if="chartTypeCode(chart) === 'SCATTER'" class="preview-svg" viewBox="0 0 320 180" aria-hidden="true">
            <circle cx="38" cy="126" r="6" class="preview-point" />
            <circle cx="82" cy="92" r="5" class="preview-point" />
            <circle cx="120" cy="112" r="7" class="preview-point" />
            <circle cx="172" cy="70" r="8" class="preview-point" />
            <circle cx="232" cy="96" r="6" class="preview-point" />
            <circle cx="286" cy="54" r="8" class="preview-point" />
          </svg>
          <svg v-else-if="chartTypeCode(chart) === 'HEATMAP'" class="preview-svg heatmap" viewBox="0 0 320 180" aria-hidden="true">
            <rect x="36" y="34" width="72" height="54" rx="10" class="heat-cell c1" />
            <rect x="110" y="34" width="72" height="54" rx="10" class="heat-cell c3" />
            <rect x="184" y="34" width="72" height="54" rx="10" class="heat-cell c2" />
            <rect x="36" y="90" width="72" height="54" rx="10" class="heat-cell c4" />
            <rect x="110" y="90" width="72" height="54" rx="10" class="heat-cell c2" />
            <rect x="184" y="90" width="72" height="54" rx="10" class="heat-cell c5" />
          </svg>
          <svg v-else-if="chartTypeCode(chart) === '3D_SURFACE'" class="preview-svg" viewBox="0 0 320 180" aria-hidden="true">
            <polyline points="28,132 68,108 112,120 162,92 212,104 292,66" class="preview-surface" />
            <polyline points="28,146 68,122 112,134 162,106 212,118 292,80" class="preview-surface low" />
            <polyline points="68,108 68,122 112,120 112,134 162,92 162,106 212,104 212,118" class="preview-grid" />
          </svg>
          <div v-else class="chart-placeholder"><Icon name="chart" :size="44" color="#0284c7" /></div>
          <p class="chart-note">{{ chartPreviewNote(chart) }}</p>
        </div>
        <div class="chart-info">
          <p class="chart-description">{{ chart.description || '暂无描述' }}</p>
          <div v-if="chartSourceInfo(chart)" class="chart-source">
            <span class="source-dot"></span>
            <span><Icon name="signal" :size="12" /> {{ chartSourceInfo(chart) }}</span>
          </div>
          <div class="chart-actions">
            <router-link :to="chartDetailPath(chart)" class="btn btn-primary">查看图表</router-link>
          </div>
        </div>
      </div>
    </div>

    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadCharts"
      @page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import Icon from '@/components/Icon.vue';
import { userApi, type ChartConfig } from '@/utils/api-user';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';
import { toPathId } from '@/utils/path-id';

/** Module D：从 dataQueryConfig 中提取数据源标签 */
const chartSourceInfo = (chart: ChartConfig): string => {
  try {
    if (!chart.dataQueryConfig) return '';
    const q: any = parseJsonPreservingLongIds(chart.dataQueryConfig);
    if (q?.dataSourceName) return String(q.dataSourceName);
    if (q?.stationId) return `站点 ${q.stationId}`;
    if (q?.dataSourceId != null) return `数据源 #${q.dataSourceId}`;
    if (Array.isArray(q?.typeCodes) && q.typeCodes.length > 0) return `指标：${q.typeCodes.join(', ')}`;
  } catch {
    // 配置 JSON 无法解析时静默跳过
  }
  return '';
};

const charts = ref<ChartConfig[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const mapStations = ref<any[]>([]);
const query = ref({
  chartType: '',
  keyword: '',
  stationKeyword: '',
  stationType: '',
  oceanRegion: '',
  dataTypeCode: '',
  minLongitude: undefined as number | undefined,
  maxLongitude: undefined as number | undefined,
  minLatitude: undefined as number | undefined,
  maxLatitude: undefined as number | undefined,
});

const chartTypeOptions = [
  { label: '折线图', value: 'LINE' },
  { label: '柱状图', value: 'BAR' },
  { label: '散点图', value: 'SCATTER' },
  { label: '热力图', value: 'HEATMAP' },
  { label: '3D 表面图', value: '3D_SURFACE' },
];

const stationTypeOptions = computed(() => {
  const map = new Map<string, string>();
  mapStations.value.forEach((s: any) => {
    const value = String(s.stationType || s.sourceType || '').trim();
    const label = String(s.stationTypeDesc || value).trim();
    if (value) map.set(value, label || value);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
});

const oceanOptions = computed(() => {
  const map = new Map<string, string>();
  mapStations.value.forEach((s: any) => {
    const value = String(s.oceanRegion || '').trim();
    const label = String(s.oceanRegionDesc || value).trim();
    if (value) map.set(value, label || value);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
});

const normalizeChartType = (raw?: unknown): 'LINE' | 'BAR' | 'SCATTER' | 'HEATMAP' | '3D_SURFACE' | 'UNKNOWN' => {
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
  return map[source] || map[upper] || 'UNKNOWN';
};

const chartTypeCode = (chart: ChartConfig): string => {
  const typedChart = chart as ChartConfig & { chartTypeCode?: string; chartTypeDesc?: string };
  const code = normalizeChartType(typedChart.chartTypeCode || typedChart.chartType || typedChart.chartTypeDesc);
  return code === 'UNKNOWN' ? 'LINE' : code;
};

const chartTypeLabel = (chart: ChartConfig): string => {
  const map: Record<string, string> = {
    LINE: '折线图',
    BAR: '柱状图',
    SCATTER: '散点图',
    HEATMAP: '热力图',
    '3D_SURFACE': '3D 表面图',
  };
  return map[chartTypeCode(chart)] || '图表';
};

const chartPreviewNote = (chart: ChartConfig): string => {
  const map: Record<string, string> = {
    LINE: '时序趋势预览',
    BAR: '柱状统计预览',
    SCATTER: '散点分布预览',
    HEATMAP: '热力密度预览',
    '3D_SURFACE': '3D 表面图预览',
  };
  return map[chartTypeCode(chart)] || '图表预览';
};

const chartDetailPath = (chart: ChartConfig): string => {
  if (chart.id == null || String(chart.id).trim() === '') return '/user/chart-gallery';
  return `/user/chart/${toPathId(chart.id)}`;
};

const loadCharts = async () => {
  loading.value = true;
  try {
    const keyword = query.value.keyword.trim();
    const stationKeyword = query.value.stationKeyword.trim();
    const dataTypeCode = query.value.dataTypeCode.trim();
    const res = await userApi.getPublicCharts({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      chartName: keyword || undefined,
      chartType: query.value.chartType || undefined,
      stationKeyword: stationKeyword || undefined,
      stationType: query.value.stationType || undefined,
      oceanRegion: query.value.oceanRegion || undefined,
      dataTypeCode: dataTypeCode || undefined,
      minLongitude: query.value.minLongitude,
      maxLongitude: query.value.maxLongitude,
      minLatitude: query.value.minLatitude,
      maxLatitude: query.value.maxLatitude,
    });
    charts.value = res.list;
    total.value = res.total;
  } catch (error) {
    console.error('加载图表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handlePageSizeChange = () => {
  pageNum.value = 1; // 页大小改变时重置到第一页
  loadCharts();
};

const handleSearch = () => {
  pageNum.value = 1;
  loadCharts();
};

const handleFilterChange = () => {
  pageNum.value = 1;
  loadCharts();
};

const clearFilters = () => {
  query.value.chartType = '';
  query.value.keyword = '';
  query.value.stationKeyword = '';
  query.value.stationType = '';
  query.value.oceanRegion = '';
  query.value.dataTypeCode = '';
  query.value.minLongitude = undefined;
  query.value.maxLongitude = undefined;
  query.value.minLatitude = undefined;
  query.value.maxLatitude = undefined;
  pageNum.value = 1;
  loadCharts();
};

const loadMapStations = async () => {
  try {
    mapStations.value = await userApi.getMapStations();
  } catch {
    mapStations.value = [];
  }
};

onMounted(() => {
  loadMapStations();
  loadCharts();
});
</script>

<style scoped lang="less">
.user-chart-gallery {
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@hairline: #e2e8f0;
@accent: #0284c7;
@accent-soft: #7dd3fc;
@paper: #ffffff;
@bg-soft: #f8fafc;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;

.text-center {
  padding: 80px 20px;
  color: @ink-3;
  font-size: 14px;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: 0.2px;
}

.filter-panel {
  margin-bottom: 20px;
  padding: 16px 18px;
  border: 1px solid @hairline;
  border-radius: 14px;
  background: @paper;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  align-items: end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 12px;
    color: @ink-2;
    font-weight: 600;
    letter-spacing: 0.2px;
  }

  select,
  input {
    width: 100%;
    height: 38px;
    border: 1px solid @hairline;
    border-radius: 10px;
    padding: 0 12px;
    font-size: 13px;
    color: @ink-2;
    background: #fff;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:focus {
      outline: none;
      border-color: rgba(2, 132, 199, 0.45);
      box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1);
    }
  }
}

.keyword-item input::placeholder {
  color: #94a3b8;
}

.filter-actions {
  display: inline-flex;
  gap: 8px;
  align-self: end;
}

.coord-item .coord-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 6px;
  align-items: center;

  span {
    font-size: 12px;
    color: #64748b;
    text-align: center;
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 28px;
  margin-bottom: 32px;
}

.chart-card {
  padding: 0;
  overflow: hidden;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 14px;
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.3s cubic-bezier(0.2, 0.8, 0.2, 1),
              border-color 0.3s ease;
  position: relative;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);

  &:hover {
    transform: translateY(-4px);
    border-color: @accent;
    box-shadow: 0 18px 40px -12px rgba(2, 132, 199, 0.22);
  }
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 18px;
}

.chart-name {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 17px;
  font-weight: 700;
  color: @ink-1;
  margin: 0;
  letter-spacing: -0.2px;
  line-height: 1.3;
}

.chart-type {
  font-size: 10.5px;
  padding: 3px 10px;
  background: rgba(2, 132, 199, 0.08);
  border-radius: 6px;
  color: @accent;
  font-weight: 600;
  border: none;
  font-family: 'Inter', sans-serif;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.chart-preview {
  height: 200px;
  background: linear-gradient(135deg, #ecf6fe 0%, #d8edfd 100%);
  display: block;
  transition: background 0.3s;
  position: relative;
  border-top: 1px solid @hairline;
  border-bottom: 1px solid @hairline;
  overflow: hidden;
}

.chart-placeholder {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  opacity: 0.85;
  animation: float 4s ease-in-out infinite;
  color: @accent;
}

.preview-svg {
  width: 100%;
  height: calc(100% - 26px);
  padding: 22px 24px;
}

.preview-line {
  fill: none;
  stroke: #0284c7;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.preview-dot {
  fill: #06b6d4;
  stroke: #ffffff;
  stroke-width: 2;
}

.preview-bar {
  fill: #0ea5e9;
  rx: 8;
  opacity: 0.85;
}

.preview-point {
  fill: #0284c7;
  opacity: 0.84;
  stroke: #ffffff;
  stroke-width: 2;
}

.heat-cell.c1 {
  fill: #bae6fd;
}

.heat-cell.c2 {
  fill: #7dd3fc;
}

.heat-cell.c3 {
  fill: #38bdf8;
}

.heat-cell.c4 {
  fill: #0ea5e9;
}

.heat-cell.c5 {
  fill: #0284c7;
}

.preview-surface {
  fill: none;
  stroke: #0284c7;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.preview-surface.low {
  stroke: rgba(2, 132, 199, 0.45);
  stroke-width: 2;
}

.preview-grid {
  fill: none;
  stroke: rgba(2, 132, 199, 0.35);
  stroke-width: 1.5;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

.chart-note {
  font-size: 11px;
  color: @ink-3;
  margin: 0;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  font-family: 'Inter', sans-serif;
  font-weight: 500;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 8px;
  text-align: center;
}

.chart-info {
  padding: 20px 24px 24px;
}

.chart-source {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 14px 0;
  padding: 4px 10px;
  background: rgba(2, 132, 199, 0.06);
  border: 1px solid rgba(2, 132, 199, 0.18);
  border-radius: 999px;
  font-size: 11.5px;
  color: @accent;
  font-weight: 500;
  font-family: 'Inter', sans-serif;
  letter-spacing: 0.1px;
}
.source-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
  flex-shrink: 0;
}

.chart-description {
  font-size: 13px;
  color: @ink-3;
  line-height: 1.65;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chart-actions {
  margin-top: 14px;

  .btn { width: 100%; }
}

// 响应式设计
@media (max-width: 768px) {
  .filter-panel {
    padding: 14px;
  }

  .filter-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .filter-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .chart-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .chart-card {
    &:hover {
      transform: translateY(-4px);
    }
  }
}
</style>

