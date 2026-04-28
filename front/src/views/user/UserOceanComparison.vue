<template>
  <div class="user-ocean-comparison">
    <div class="header card">
      <div class="page-eyebrow"><span class="dash"></span><span>MULTI · STATION DIFF</span></div>
      <h1><Icon name="bars" :size="22" color="#00d4ff" /> 多站点对比分析</h1>
      <p>同时选择 2 个或以上站点，系统会自动识别空间异常（某站显著偏离群体均值 &gt;2σ）</p>
      <div class="filters">
        <label class="multi-select">
          <span>对比站点（可多选，Ctrl/Cmd 点击）</span>
          <select v-model="form.dataSourceIds" multiple size="6">
            <option v-for="ds in dataSourceList" :key="String(ds.id)" :value="String(ds.id)">
              {{ ds.sourceName || ds.stationId || ds.id }}
            </option>
          </select>
        </label>
        <div class="filter-col">
          <label>
            <span>开始时间</span>
            <input v-model="form.startTime" type="datetime-local" step="3600" />
          </label>
          <label>
            <span>结束时间</span>
            <input v-model="form.endTime" type="datetime-local" step="3600" />
          </label>
          <label>
            <span>回看小时</span>
            <input v-model.number="form.historyHours" type="number" min="1" max="720" />
          </label>
          <button class="btn btn-primary" :disabled="loading || form.dataSourceIds.length < 2" @click="doCompare">
            {{ loading ? '对比中...' : `对比 ${form.dataSourceIds.length} 个站点` }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="errorMsg" class="error card">{{ errorMsg }}</div>

    <div v-if="result" class="result-grid">
      <!-- 群体指标 -->
      <section class="card">
        <h3><Icon name="bars" :size="16" color="#00d4ff" /> 群体指标统计</h3>
        <table class="stat-table">
          <thead>
            <tr>
              <th>指标</th>
              <th>最小</th>
              <th>最大</th>
              <th>均值</th>
              <th>极差</th>
              <th>标准差</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in metricRows" :key="m.key">
              <td class="metric-name">{{ m.label }}</td>
              <td>{{ fmt(m.stat?.min) }}</td>
              <td>{{ fmt(m.stat?.max) }}</td>
              <td>{{ fmt(m.stat?.mean) }}</td>
              <td>{{ fmt(m.stat?.range) }}</td>
              <td>{{ fmt(m.stat?.stddev) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 空间异常 -->
      <section class="card">
        <h3><Icon name="alert" :size="16" color="#ffc868" /> 空间异常检测</h3>
        <p v-if="!result.anomalies || result.anomalies.length === 0" class="no-anomaly">
          <Icon name="check" :size="14" color="#5be3a8" />
          所有站点均在群体均值 ±2σ 范围内，未检测到显著空间异常
        </p>
        <ul v-else class="anomaly-list">
          <li v-for="(a, idx) in result.anomalies" :key="idx" :class="'severity-' + (a.severity || 'HIGH').toLowerCase()">
            <div class="anomaly-title">
              <span class="anomaly-badge">{{ a.severity === 'HIGH' ? '偏高' : '偏低' }}</span>
              <span>{{ a.sourceName || '站点#' + a.dataSourceId }}</span>
              <span class="metric-tag">{{ metricLabel(a.metric) }}</span>
            </div>
            <div class="anomaly-desc">{{ a.description }}</div>
            <div class="anomaly-detail">
              值 = <b>{{ fmt(a.value) }}</b>，群体均值 = {{ fmt(a.groupMean) }}，
              z = <b :class="'z-' + (a.severity || 'HIGH').toLowerCase()">{{ fmt(a.zScore) }}σ</b>
            </div>
          </li>
        </ul>
      </section>

      <!-- 站点对照表 -->
      <section class="card full-width">
        <h3><Icon name="list" :size="16" color="#00d4ff" /> 站点对照表</h3>
        <div class="table-scroll">
          <table class="station-table">
            <thead>
              <tr>
                <th>站点</th>
                <th>位置</th>
                <th>温度(°C)</th>
                <th>波高(m)</th>
                <th>风速(m/s)</th>
                <th>稳定性</th>
                <th>异常</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in result.stations || []" :key="String(s.dataSourceId)">
                <td class="station-name">
                  <b>{{ s.sourceName || s.stationId || '#' + s.dataSourceId }}</b>
                  <small v-if="s.stationId">{{ s.stationId }}</small>
                </td>
                <td class="geo">
                  <span v-if="s.longitude != null && s.latitude != null">
                    {{ Number(s.latitude).toFixed(2) }}°, {{ Number(s.longitude).toFixed(2) }}°
                  </span>
                  <span v-else class="na">—</span>
                </td>
                <td>{{ fmt(s.temperature) }}</td>
                <td>{{ fmt(s.waveHeight) }}</td>
                <td>{{ fmt(s.windSpeed) }}</td>
                <td>{{ fmt(s.stabilityIndex) }}</td>
                <td>
                  <span v-if="s.abnormal" class="abnormal"><Icon name="alert" :size="12" /> 有告警</span>
                  <span v-else class="normal"><Icon name="check" :size="12" /> 正常</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="confidence-line">
          <span>对比置信度：</span>
          <b :class="'confidence-' + (result.confidence || 'LOW').toLowerCase()">
            {{ result.confidence || '—' }}
          </b>
          <span v-if="result.degradeReason && result.degradeReason.length" class="degrade-reason">
            · {{ result.degradeReason.join('；') }}
          </span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { userApi, type DataSourceItem, type OceanComparisonResult, type MetricStat } from '@/utils/api-user';
import Icon from '@/components/Icon.vue';

const dataSourceList = ref<DataSourceItem[]>([]);
const loading = ref(false);
const errorMsg = ref('');
const result = ref<OceanComparisonResult | null>(null);

const form = reactive({
  dataSourceIds: [] as string[],
  startTime: '',
  endTime: '',
  historyHours: 24,
});

const fmt = (v: any): string => {
  if (v == null || v === '') return '—';
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return n.toFixed(2);
};

const metricLabel = (m?: string): string => {
  switch (m) {
    case 'TEMPERATURE': return '温度';
    case 'WAVE_HEIGHT': return '波高';
    case 'WIND_SPEED': return '风速';
    case 'STABILITY': return '稳定性';
    default: return m || '—';
  }
};

const metricRows = computed(() => {
  const r = result.value;
  if (!r || !r.metrics) return [];
  return [
    { key: 'temperature', label: '温度 (°C)', stat: r.metrics.temperature as MetricStat | undefined },
    { key: 'waveHeight', label: '波高 (m)', stat: r.metrics.waveHeight as MetricStat | undefined },
    { key: 'windSpeed', label: '风速 (m/s)', stat: r.metrics.windSpeed as MetricStat | undefined },
    { key: 'stabilityIndex', label: '稳定性指数', stat: r.metrics.stabilityIndex as MetricStat | undefined },
  ];
});

const loadDataSources = async () => {
  try {
    const list = await userApi.getUserDataSourceList();
    dataSourceList.value = list || [];
  } catch (e) {
    console.error('加载数据源失败', e);
  }
};

const toBackendTime = (v?: string): string | undefined => {
  if (!v) return undefined;
  return v.replace('T', ' ') + (v.length === 16 ? ':00' : '');
};

const doCompare = async () => {
  if (form.dataSourceIds.length < 2) {
    errorMsg.value = '请至少选择 2 个站点';
    return;
  }
  errorMsg.value = '';
  loading.value = true;
  try {
    const res = await userApi.analyzeOceanCompare({
      dataSourceIds: form.dataSourceIds,
      startTime: toBackendTime(form.startTime),
      endTime: toBackendTime(form.endTime),
      historyHours: form.historyHours,
    });
    result.value = res;
  } catch (e: any) {
    errorMsg.value = e?.message || '对比分析失败';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadDataSources();
});
</script>

<style scoped lang="less">
.user-ocean-comparison {
  padding: 24px;
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.card {
  background: rgba(15, 20, 45, 0.7);
  border: 1px solid rgba(102, 126, 234, 0.25);
  border-radius: 12px;
  padding: 20px 24px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 16px;
}
.card h1 {
  font-size: 22px;
  margin: 0 0 6px;
  color: #e0f2ff;
  font-weight: 600;
  letter-spacing: 0.4px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.card h3 {
  font-size: 14px;
  margin: 0 0 14px;
  color: #cde3ff;
  font-weight: 600;
  letter-spacing: 0.4px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.card p { margin: 0 0 14px; color: rgba(255, 255, 255, 0.6); font-size: 13px; line-height: 1.6; letter-spacing: 0.3px; }
.page-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 2.4px;
  color: rgba(0, 212, 255, 0.7);
}
.page-eyebrow .dash {
  width: 28px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #00d4ff);
}

.filters {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
  margin-top: 12px;
}
.multi-select select {
  width: 100%;
  padding: 10px;
  background: rgba(10, 14, 39, 0.6);
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.35);
  border-radius: 8px;
  font-size: 13px;
}
.filter-col { display: flex; flex-direction: column; gap: 8px; }
.filter-col label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: rgba(200, 220, 255, 0.8); }
.filter-col input {
  padding: 8px 10px;
  background: rgba(10, 14, 39, 0.6);
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.35);
  border-radius: 6px;
  font-size: 13px;
}

.btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.35);
  background: rgba(102, 126, 234, 0.2);
  color: rgba(255, 255, 255, 0.95);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 6px;
}
.btn-primary { background: linear-gradient(135deg, #667eea, #764ba2); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.error {
  background: rgba(255, 100, 100, 0.1);
  border-color: rgba(255, 100, 100, 0.4);
  color: #ff8888;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.full-width { grid-column: 1 / -1; }

.stat-table, .station-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.stat-table th, .stat-table td,
.station-table th, .station-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid rgba(102, 126, 234, 0.15);
}
.stat-table th, .station-table th {
  background: rgba(102, 126, 234, 0.12);
  color: #cde3ff;
  font-weight: 600;
}
.metric-name { color: rgba(200, 220, 255, 0.9); font-weight: 500; }
.station-name b { color: #cde3ff; }
.station-name small { display: block; color: rgba(200, 220, 255, 0.6); margin-top: 2px; }
.geo { font-family: 'Consolas', monospace; font-size: 12px; color: rgba(92, 246, 196, 0.9); }
.na { color: rgba(255, 255, 255, 0.4); }

.no-anomaly {
  padding: 16px;
  background: rgba(92, 246, 196, 0.1);
  border: 1px dashed rgba(92, 246, 196, 0.4);
  border-radius: 8px;
  color: rgba(200, 255, 230, 0.95);
  font-size: 13px;
}
.anomaly-list { list-style: none; padding: 0; margin: 0; }
.anomaly-list li {
  padding: 12px 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  border-left: 4px solid;
  background: rgba(10, 14, 39, 0.45);
}
.severity-high { border-left-color: #ff6b6b; }
.severity-low { border-left-color: #5e95ff; }
.anomaly-title { display: flex; align-items: center; gap: 10px; font-weight: 600; color: #cde3ff; margin-bottom: 4px; }
.anomaly-badge {
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 107, 107, 0.2);
  border: 1px solid rgba(255, 107, 107, 0.4);
  color: #ff8888;
  font-size: 11px;
}
.severity-low .anomaly-badge {
  background: rgba(94, 149, 255, 0.2);
  border-color: rgba(94, 149, 255, 0.4);
  color: #88aaff;
}
.metric-tag {
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.15);
  font-size: 11px;
  color: rgba(200, 220, 255, 0.85);
}
.anomaly-desc { font-size: 13px; color: rgba(255, 255, 255, 0.85); margin-bottom: 4px; }
.anomaly-detail { font-size: 12px; color: rgba(200, 220, 255, 0.7); font-family: 'Consolas', monospace; }
.z-high { color: #ff6b6b; }
.z-low { color: #5e95ff; }

.abnormal { color: #ff9c5e; font-size: 12px; font-weight: 500; }
.normal { color: #5cf6c4; font-size: 12px; }

.table-scroll { overflow-x: auto; }

.confidence-line {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(200, 220, 255, 0.7);
}
.confidence-high { color: #5cf6c4; }
.confidence-medium { color: #ffcb5e; }
.confidence-low { color: #ff6b6b; }
.degrade-reason { color: rgba(255, 200, 120, 0.85); }

/* 浅色主色调可读性覆盖 */
.card {
  background: #ffffff;
  border: 1px solid #dbe8f4;
  color: #334155;
}

.card h1 {
  color: #0f172a;
}

.card h3 {
  color: #0f172a;
}

.card p {
  color: #64748b;
}

.page-eyebrow {
  color: rgba(2, 132, 199, 0.78);
}

.page-eyebrow .dash {
  background: linear-gradient(90deg, transparent, #0284c7);
}

.multi-select select,
.filter-col input {
  background: #ffffff;
  color: #0f172a;
  border: 1px solid #dbe8f4;
}

.filter-col label {
  color: #475569;
}

.btn {
  border-color: #dbe8f4;
  background: #f8fbff;
  color: #334155;
}

.btn-primary {
  background: linear-gradient(135deg, #0284c7, #06b6d4);
  color: #ffffff;
}

.error {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #dc2626;
}

.stat-table th,
.station-table th {
  background: #f4f9ff;
  color: #334155;
}

.stat-table th,
.stat-table td,
.station-table th,
.station-table td {
  border-bottom: 1px solid #e2e8f0;
}

.metric-name,
.station-name b {
  color: #0f172a;
}

.station-name small {
  color: #64748b;
}

.geo {
  color: #0369a1;
}

.na {
  color: #94a3b8;
}

.no-anomaly {
  background: #ecfdf5;
  border-color: #86efac;
  color: #166534;
}

.anomaly-list li {
  background: #f8fbff;
  border: 1px solid #dbe8f4;
}

.anomaly-title {
  color: #0f172a;
}

.metric-tag {
  color: #334155;
  background: #e0f2fe;
}

.anomaly-desc {
  color: #334155;
}

.anomaly-detail,
.confidence-line {
  color: #64748b;
}

.degrade-reason {
  color: #b45309;
}

@media (max-width: 1024px) {
  .filters { grid-template-columns: 1fr; }
  .result-grid { grid-template-columns: 1fr; }
}
</style>
