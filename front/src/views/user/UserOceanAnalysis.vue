<template>
  <div class="user-ocean-analysis">
    <div class="header card">
      <h1>海洋环境辅助决策分析</h1>
      <p>基于实测数据计算异常预警、稳定性、舒适度和短期趋势</p>
      <div class="filters">
        <label>
          <span>数据源</span>
          <select v-model="form.dataSourceId">
            <option value="">请选择数据源</option>
            <option v-for="ds in dataSourceList" :key="String(ds.id)" :value="String(ds.id)">
              {{ ds.sourceName || ds.stationId || ds.id }}
            </option>
          </select>
        </label>
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
        <button class="btn btn-primary" :disabled="loading" @click="loadAll">
          {{ loading ? '计算中...' : '开始分析' }}
        </button>
        <button class="btn btn-default" :disabled="!composite" @click="exportJson">导出JSON</button>
        <button class="btn btn-default" :disabled="!composite" @click="exportCsv">导出CSV</button>
      </div>
    </div>

    <div v-if="errorMsg" class="error card">{{ errorMsg }}</div>

    <div class="grid">
      <section class="card">
        <h3>环境异常预警</h3>
        <div class="alarm-strip">
          <span :class="alarmClass('safe')">安全</span>
          <span :class="alarmClass('warn')">注意</span>
          <span :class="alarmClass('danger')">危险</span>
        </div>
        <div class="kv"><span>一级告警</span><b :class="abnormal?.level1Alarm ? 'danger' : 'ok'">{{ abnormal?.level1Alarm ? '触发' : '正常' }}</b></div>
        <div class="kv"><span>二级告警</span><b :class="abnormal?.level2Alarm ? 'warn' : 'ok'">{{ abnormal?.level2Alarm ? '触发' : '正常' }}</b></div>
        <div class="kv"><span>15分钟前参考</span><b>{{ abnormal?.ref15mStatus || '-' }}</b></div>
        <div class="kv"><span>可信度</span><b>{{ abnormal?.confidence || '-' }}</b></div>
        <p class="summary">{{ alarmSummary }}</p>
        <p v-if="abnormal?.degradeReason?.length" class="hint">{{ abnormal.degradeReason.join('；') }}</p>
      </section>

      <section class="card">
        <h3>环境稳定性</h3>
        <div class="gauge-wrap">
          <svg viewBox="0 0 240 140" class="gauge">
            <path d="M20 120 A100 100 0 0 1 69.99999999999996 33.39745962155614" class="gauge-seg low" />
            <path d="M69.99999999999996 33.39745962155614 A100 100 0 0 1 170 33.39745962155614" class="gauge-seg mid" />
            <path d="M170 33.39745962155614 A100 100 0 0 1 220 120" class="gauge-seg high" />
            <path :d="stabilityGaugePath" class="gauge-fg stability" />
            <text x="120" y="88" text-anchor="middle" class="gauge-value">{{ stability?.stabilityIndex ?? '-' }}</text>
            <text x="60" y="128" text-anchor="middle" class="gauge-tick">60</text>
            <text x="120" y="28" text-anchor="middle" class="gauge-tick">80</text>
            <text x="182" y="128" text-anchor="middle" class="gauge-tick">90</text>
          </svg>
        </div>
        <div class="badge-row"><span class="badge">{{ stabilityBadge }}</span></div>
        <div class="kv"><span>等级</span><b>{{ stability?.level || '-' }}</b></div>
        <div class="kv"><span>可信度</span><b>{{ stability?.confidence || '-' }}</b></div>
        <p v-if="stability?.degradeReason?.length" class="hint">{{ stability.degradeReason.join('；') }}</p>
      </section>

      <section class="card">
        <h3>舒适度评分</h3>
        <div class="gauge-wrap">
          <svg viewBox="0 0 240 140" class="gauge">
            <path d="M20 120 A100 100 0 0 1 69.99999999999996 33.39745962155614" class="gauge-seg low" />
            <path d="M69.99999999999996 33.39745962155614 A100 100 0 0 1 170 33.39745962155614" class="gauge-seg mid" />
            <path d="M170 33.39745962155614 A100 100 0 0 1 220 120" class="gauge-seg high" />
            <path :d="comfortGaugePath" class="gauge-fg comfort" />
            <text x="120" y="88" text-anchor="middle" class="gauge-value">{{ comfort?.score ?? '-' }}</text>
            <text x="60" y="128" text-anchor="middle" class="gauge-tick">60</text>
            <text x="120" y="28" text-anchor="middle" class="gauge-tick">80</text>
            <text x="182" y="128" text-anchor="middle" class="gauge-tick">90</text>
          </svg>
        </div>
        <div class="badge-row"><span class="badge">{{ comfortBadge }}</span></div>
        <div class="kv"><span>等级</span><b>{{ comfort?.level || '-' }}</b></div>
        <div class="kv"><span>建议</span><b>{{ comfort?.suggestion || '-' }}</b></div>
        <p v-if="comfort?.degradeReason?.length" class="hint">{{ comfort.degradeReason.join('；') }}</p>
      </section>

      <section class="card">
        <h3>短期趋势预测</h3>
        <div class="kv"><span>温度预测</span><b>{{ trend?.temperaturePredict ?? '-' }}</b></div>
        <div class="kv"><span>温度趋势</span><b>{{ trend?.temperatureTrend || '-' }}</b></div>
        <div class="kv"><span>潮位预测</span><b>{{ trend?.seaLevelPredict ?? '-' }}</b></div>
        <div class="kv"><span>潮位趋势</span><b>{{ trend?.seaLevelTrend || '-' }}</b></div>
        <p v-if="trend?.degradeReason?.length" class="hint">{{ trend.degradeReason.join('；') }}</p>
      </section>
    </div>

    <section class="card ai-card">
      <div class="ai-head">
        <h3>
          <span class="ai-mark"><Icon name="brain" :size="18" color="#00d4ff" /></span>
          DeepSeek AI 智能预测
          <span class="ai-tag">LLM · 24H FORECAST</span>
        </h3>
        <button class="btn btn-primary" :disabled="aiLoading || !composite" @click="runAiForecast">
          <Icon v-if="!aiLoading" name="bolt" :size="14" />
          <Icon v-else name="refresh" :size="14" class="spin" />
          <span>{{ aiLoading ? 'AI 思考中…' : (aiResult?.content ? '重新生成' : '让 AI 给出 24h 预测') }}</span>
        </button>
      </div>
      <p class="hint" v-if="!composite">请先点击"开始分析"加载站点海况，再调用 AI 预测。</p>
      <p class="hint" v-else-if="!aiResult">基于当前站点的实测海况快照与近 24h 趋势，由 DeepSeek 大模型给出 6h/12h/24h 海况预测和作业建议。</p>
      <div v-if="aiResult?.error" class="error">
        <Icon name="alert" :size="14" />
        <span>{{ aiResult.error }}</span>
      </div>
      <div v-if="aiResult?.content" class="ai-content" v-html="aiHtml"></div>
      <div v-if="aiResult?.totalTokens" class="ai-meta">
        <span>模型：{{ aiResult.model || '-' }}</span>
        <span>耗时：{{ aiResult.elapsedMs }} ms</span>
        <span>tokens：{{ aiResult.promptTokens }} + {{ aiResult.completionTokens }} = {{ aiResult.totalTokens }}</span>
      </div>
    </section>

    <section class="card">
      <h3>综合趋势图（历史+预测）</h3>
      <div class="chart-wrap">
        <div class="chart-block">
          <h4>温度趋势（TEMP/SST）</h4>
          <svg viewBox="0 0 600 180" class="sparkline" @mousemove="onTempMove" @mouseleave="hoverTemp = null">
            <polyline :points="tempLinePoints" fill="none" stroke="#4ecdc4" stroke-width="2" />
            <line v-if="hoverTemp" :x1="hoverTemp.x" y1="10" :x2="hoverTemp.x" y2="170" stroke="rgba(255,255,255,.35)" stroke-width="1" />
          </svg>
          <div v-if="hoverTemp" class="axis-tip">{{ hoverTemp.label }}</div>
          <div class="axis-labels">
            <span>{{ tempAxisStart }}</span>
            <span>{{ tempAxisEnd }}</span>
          </div>
          <div class="kv"><span>预测值</span><b>{{ trend?.temperaturePredict ?? '-' }}</b></div>
          <div class="kv"><span>趋势</span><b>{{ trend?.temperatureTrend || '-' }}</b></div>
        </div>
        <div class="chart-block">
          <h4>潮位趋势（SEA_LEVEL）</h4>
          <svg viewBox="0 0 600 180" class="sparkline" @mousemove="onTideMove" @mouseleave="hoverTide = null">
            <polyline :points="tideLinePoints" fill="none" stroke="#5b8ff9" stroke-width="2" />
            <line v-if="hoverTide" :x1="hoverTide.x" y1="10" :x2="hoverTide.x" y2="170" stroke="rgba(255,255,255,.35)" stroke-width="1" />
          </svg>
          <div v-if="hoverTide" class="axis-tip">{{ hoverTide.label }}</div>
          <div class="axis-labels">
            <span>{{ tideAxisStart }}</span>
            <span>{{ tideAxisEnd }}</span>
          </div>
          <div class="kv"><span>预测值</span><b>{{ trend?.seaLevelPredict ?? '-' }}</b></div>
          <div class="kv"><span>趋势</span><b>{{ trend?.seaLevelTrend || '-' }}</b></div>
        </div>
      </div>
      <p v-if="composite?.series?.length" class="hint">历史样本点：{{ composite.series.length }}（来自综合分析接口）</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { userApi, type DataSourceItem, type OceanAbnormalResult, type OceanComfortResult, type OceanCompositeResult, type OceanStabilityResult, type OceanTrendResult } from '@/utils/api-user';
import Icon from '@/components/Icon.vue';

const loading = ref(false);
const errorMsg = ref('');
const aiLoading = ref(false);
const aiResult = ref<Awaited<ReturnType<typeof userApi.aiForecast>> | null>(null);
const abnormal = ref<OceanAbnormalResult | null>(null);
const stability = ref<OceanStabilityResult | null>(null);
const comfort = ref<OceanComfortResult | null>(null);
const trend = ref<OceanTrendResult | null>(null);
const composite = ref<OceanCompositeResult | null>(null);
const dataSourceList = ref<DataSourceItem[]>([]);
const hoverTemp = ref<{ x: number; label: string } | null>(null);
const hoverTide = ref<{ x: number; label: string } | null>(null);

const form = ref({
  dataSourceId: '',
  startTime: '',
  endTime: '',
  historyHours: 24,
});

const loadAll = async () => {
  errorMsg.value = '';
  if (!form.value.dataSourceId) {
    errorMsg.value = '请先填写数据源ID';
    return;
  }
  try {
    loading.value = true;
    const req = {
      dataSourceId: form.value.dataSourceId,
      startTime: form.value.startTime || undefined,
      endTime: form.value.endTime || undefined,
      historyHours: form.value.historyHours || 24,
    };
    const comp = await userApi.analyzeOceanComposite(req);
    composite.value = comp;
    abnormal.value = comp?.abnormal || null;
    stability.value = comp?.stability || null;
    comfort.value = comp?.comfort || null;
    trend.value = comp?.trend || null;
  } catch (error: any) {
    errorMsg.value = error?.message || '分析失败，请检查参数或数据源';
  } finally {
    loading.value = false;
  }
};

const loadDataSources = async () => {
  try {
    dataSourceList.value = await userApi.getUserDataSourceList();
    if (!form.value.dataSourceId && dataSourceList.value.length > 0) {
      form.value.dataSourceId = String(dataSourceList.value[0].id ?? '');
    }
  } catch (e) {
    // ignore list load failure
  }
};

const makePoints = (values: number[]) => {
  if (!values.length) return '';
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const width = 580;
  const height = 160;
  return values
    .map((v, idx) => {
      const x = 10 + (idx * width) / Math.max(values.length - 1, 1);
      const y = 10 + ((max - v) / range) * (height - 20);
      return `${x},${y}`;
    })
    .join(' ');
};

const formatTime = (v?: string) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-');

const tempSeries = computed(() => (composite.value?.series || [])
  .map((s: any) => ({ t: String(s.time || ''), v: Number(s.temperature) }))
  .filter((s: any) => !Number.isNaN(s.v)));
const tideSeries = computed(() => (composite.value?.series || [])
  .map((s: any) => ({ t: String(s.time || ''), v: Number(s.seaLevel) }))
  .filter((s: any) => !Number.isNaN(s.v)));

const tempLinePoints = computed(() => {
  return makePoints(tempSeries.value.map((s: any) => s.v));
});

const tideLinePoints = computed(() => {
  return makePoints(tideSeries.value.map((s: any) => s.v));
});

const tempAxisStart = computed(() => formatTime(tempSeries.value[0]?.t));
const tempAxisEnd = computed(() => formatTime(tempSeries.value[tempSeries.value.length - 1]?.t));
const tideAxisStart = computed(() => formatTime(tideSeries.value[0]?.t));
const tideAxisEnd = computed(() => formatTime(tideSeries.value[tideSeries.value.length - 1]?.t));

const gaugePathByScore = (score?: number) => {
  const v = Math.max(0, Math.min(100, Number(score ?? 0)));
  const angle = Math.PI * (1 - v / 100);
  const x = 120 + 100 * Math.cos(angle);
  const y = 120 - 100 * Math.sin(angle);
  const largeArc = v > 50 ? 1 : 0;
  return `M20 120 A100 100 0 ${largeArc} 1 ${x} ${y}`;
};

const stabilityGaugePath = computed(() => gaugePathByScore(Number(stability.value?.stabilityIndex ?? 0)));
const comfortGaugePath = computed(() => gaugePathByScore(Number(comfort.value?.score ?? 0)));

const alarmClass = (level: 'safe' | 'warn' | 'danger') => {
  const l1 = !!abnormal.value?.level1Alarm;
  const l2 = !!abnormal.value?.level2Alarm;
  const current = l1 ? 'danger' : (l2 ? 'warn' : 'safe');
  return ['alarm-level', level, current === level ? 'active' : ''].join(' ');
};
const alarmSummary = computed(() => {
  const l1 = !!abnormal.value?.level1Alarm;
  const l2 = !!abnormal.value?.level2Alarm;
  if (l1) return '当前风险等级：危险，建议暂停高风险海上活动。';
  if (l2) return '当前风险等级：注意，建议谨慎开展海上活动。';
  return '当前风险等级：安全，可按常规安排海上活动。';
});
const stabilityBadge = computed(() => `稳定性等级：${stability.value?.level || '-'}`);
const comfortBadge = computed(() => `舒适度等级：${comfort.value?.level || '-'}`);

const buildHover = (evt: MouseEvent, series: Array<{ t: string; v: number }>) => {
  if (!series.length) return null;
  const target = evt.currentTarget as SVGElement;
  const rect = target.getBoundingClientRect();
  const ratio = Math.max(0, Math.min(1, (evt.clientX - rect.left) / rect.width));
  const idx = Math.min(series.length - 1, Math.round(ratio * (series.length - 1)));
  const point = series[idx];
  const x = 10 + (idx * 580) / Math.max(series.length - 1, 1);
  return { x, label: `${formatTime(point.t)} | ${point.v.toFixed(2)}` };
};

const onTempMove = (evt: MouseEvent) => {
  hoverTemp.value = buildHover(evt, tempSeries.value);
};

const onTideMove = (evt: MouseEvent) => {
  hoverTide.value = buildHover(evt, tideSeries.value);
};

const download = (filename: string, content: string, mime = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

const exportJson = () => {
  if (!composite.value) return;
  const payload = {
    request: { ...form.value },
    abnormal: abnormal.value,
    stability: stability.value,
    comfort: comfort.value,
    trend: trend.value,
    composite: composite.value,
    exportedAt: new Date().toISOString(),
  };
  download(`ocean-analysis-${Date.now()}.json`, JSON.stringify(payload, null, 2), 'application/json;charset=utf-8');
};

const exportCsv = () => {
  const rows = composite.value?.series || [];
  if (!rows.length) return;
  const header = ['time', 'temperature', 'salinity', 'seaLevel', 'waveHeight', 'windSpeed'];
  const body = rows.map((r: any) => [
    r.time ?? '',
    r.temperature ?? '',
    r.salinity ?? '',
    r.seaLevel ?? '',
    r.waveHeight ?? '',
    r.windSpeed ?? '',
  ].join(','));
  download(`ocean-analysis-series-${Date.now()}.csv`, [header.join(','), ...body].join('\n'), 'text/csv;charset=utf-8');
};

/** 极简 Markdown→HTML 渲染（够用）：支持 #、##、###、* 列表、粗体、换行 */
const renderMarkdown = (md: string): string => {
  const escape = (s: string) => s.replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' } as any)[c]);
  const lines = String(md || '').split(/\r?\n/);
  const out: string[] = [];
  let inList = false;
  for (let raw of lines) {
    const line = raw.trim();
    if (!line) { if (inList) { out.push('</ul>'); inList = false; } out.push(''); continue; }
    let m: RegExpMatchArray | null;
    if ((m = line.match(/^###\s+(.*)$/))) { if (inList) { out.push('</ul>'); inList = false; } out.push(`<h4>${escape(m[1])}</h4>`); continue; }
    if ((m = line.match(/^##\s+(.*)$/)))  { if (inList) { out.push('</ul>'); inList = false; } out.push(`<h3>${escape(m[1])}</h3>`); continue; }
    if ((m = line.match(/^#\s+(.*)$/)))   { if (inList) { out.push('</ul>'); inList = false; } out.push(`<h2>${escape(m[1])}</h2>`); continue; }
    if ((m = line.match(/^[*\-]\s+(.*)$/))) {
      if (!inList) { out.push('<ul>'); inList = true; }
      out.push(`<li>${escape(m[1]).replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')}</li>`);
      continue;
    }
    if (inList) { out.push('</ul>'); inList = false; }
    out.push(`<p>${escape(line).replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')}</p>`);
  }
  if (inList) out.push('</ul>');
  return out.join('\n');
};
const aiHtml = computed(() => renderMarkdown(aiResult.value?.content || ''));

const runAiForecast = async () => {
  if (!form.value.dataSourceId) {
    errorMsg.value = '请先选择数据源';
    return;
  }
  aiLoading.value = true;
  try {
    aiResult.value = await userApi.aiForecast({
      dataSourceId: form.value.dataSourceId,
      startTime: form.value.startTime || undefined,
      endTime: form.value.endTime || undefined,
      historyHours: form.value.historyHours || 24,
    });
  } catch (e: any) {
    aiResult.value = { error: e?.message || 'AI 预测请求失败' };
  } finally {
    aiLoading.value = false;
  }
};

const route = useRoute();

onMounted(async () => {
  // URL query 的 dataSourceId 优先（由 Module H 站点地图跳转时携带）
  const urlDsId = route.query.dataSourceId as string | undefined;
  if (urlDsId) {
    form.value.dataSourceId = urlDsId;
  }
  await loadDataSources();
  if (urlDsId) {
    // 站点地图携带 dataSourceId 时自动分析
    loadAll();
  }
});
</script>

<style scoped lang="less">
.user-ocean-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.header h1 { margin: 0 0 8px; color: #fff; }
.header p { margin: 0 0 12px; color: rgba(255,255,255,.8); }
.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  align-items: end;
}
label { display: flex; flex-direction: column; gap: 6px; color: rgba(255,255,255,.85); font-size: 13px; }
input {
  height: 34px; border-radius: 8px; border: 1px solid rgba(102,126,234,.4);
  background: rgba(0,0,0,.2); color: #fff; padding: 0 10px;
}
select {
  height: 34px; border-radius: 8px; border: 1px solid rgba(102,126,234,.4);
  background: rgba(0,0,0,.2); color: #fff; padding: 0 10px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}
.kv { display: flex; justify-content: space-between; margin: 8px 0; color: rgba(255,255,255,.85); }
.score { font-size: 34px; font-weight: 700; color: #fff; margin: 6px 0 10px; }
.ok { color: #3ddc97; }
.warn { color: #ffb547; }
.danger { color: #ff6b6b; }
.hint { color: rgba(255,200,140,.9); font-size: 12px; line-height: 1.6; margin: 8px 0 0; }
.summary { color: rgba(220,240,255,.92); font-size: 13px; margin: 8px 0 0; }
.error { color: #ffb3b3; }
.chart-wrap { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 14px; }
.chart-block h4 { margin: 0 0 8px; color: #fff; }
.sparkline { width: 100%; height: 180px; background: rgba(8, 16, 38, 0.45); border-radius: 8px; border: 1px solid rgba(102,126,234,.25); }
.axis-labels { display: flex; justify-content: space-between; color: rgba(255,255,255,.65); font-size: 12px; margin-top: 6px; }
.axis-tip { color: #ffd699; font-size: 12px; margin-top: 6px; }
.alarm-strip { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 10px 0 12px; }
.alarm-level { text-align: center; padding: 6px 0; border-radius: 6px; font-size: 12px; opacity: .45; border: 1px solid transparent; }
.alarm-level.safe { background: rgba(61,220,151,.18); color: #7ff0bf; }
.alarm-level.warn { background: rgba(255,181,71,.18); color: #ffcc7a; }
.alarm-level.danger { background: rgba(255,107,107,.18); color: #ff9f9f; }
.alarm-level.active { opacity: 1; border-color: rgba(255,255,255,.35); box-shadow: 0 0 10px rgba(255,255,255,.15) inset; }
.gauge-wrap { display: flex; justify-content: center; margin-bottom: 4px; }
.gauge { width: 100%; max-width: 300px; height: 120px; }
.gauge-seg { fill: none; stroke-width: 12; stroke-linecap: round; opacity: .75; }
.gauge-seg.low { stroke: rgba(255,107,107,.7); }
.gauge-seg.mid { stroke: rgba(255,181,71,.7); }
.gauge-seg.high { stroke: rgba(61,220,151,.72); }
.gauge-fg { fill: none; stroke-width: 14; stroke-linecap: round; }
.gauge-fg.stability { stroke: #5b8ff9; }
.gauge-fg.comfort { stroke: #4ecdc4; }
.gauge-value { fill: #fff; font-size: 28px; font-weight: 700; }
.gauge-tick { fill: rgba(255,255,255,.68); font-size: 11px; }
.badge-row { display: flex; justify-content: center; margin: 4px 0 8px; }
.badge { padding: 4px 10px; border-radius: 12px; background: rgba(91,143,249,.2); border: 1px solid rgba(91,143,249,.4); color: #dfeeff; font-size: 12px; }

/* DeepSeek AI 卡片 */
.ai-card {
  background: linear-gradient(135deg, rgba(102,126,234,.18), rgba(118,75,162,.14));
  border: 1px solid rgba(146,180,255,.35);
  position: relative;
  overflow: hidden;
}
.ai-card::before {
  content: '';
  position: absolute; inset: 0;
  background: radial-gradient(600px 200px at 0% 0%, rgba(142, 197, 255, .14), transparent 60%);
  pointer-events: none;
}
.ai-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.ai-head h3 {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: #fff;
  letter-spacing: .3px;
  font-size: 16px;
  font-weight: 600;
}
.ai-head .ai-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(0,212,255,.18), rgba(102,126,234,.14));
  border: 1px solid rgba(0,212,255,.35);
  box-shadow: 0 0 14px rgba(0,212,255,.18);
}
.ai-head .ai-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 1.6px;
  color: rgba(0,212,255,.7);
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid rgba(0,212,255,.3);
  background: rgba(0,212,255,.06);
}
.ai-head .btn { display: inline-flex; align-items: center; gap: 6px; }
.ai-head .btn .spin { animation: aiSpin 1s linear infinite; }
@keyframes aiSpin { to { transform: rotate(360deg); } }
.ai-card .error {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #ffb3b3;
  background: rgba(255, 122, 144, .08);
  border: 1px solid rgba(255, 122, 144, .3);
  padding: 8px 12px;
  border-radius: 8px;
  margin-top: 10px;
}
.ai-content {
  margin-top: 12px;
  padding: 14px 16px;
  background: rgba(8, 16, 38, 0.55);
  border: 1px solid rgba(102,126,234,.3);
  border-radius: 10px;
  color: rgba(230,240,255,.92);
  line-height: 1.75;
  font-size: 14px;
  :deep(h2) { font-size: 18px; color: #ffd180; margin: 6px 0 8px; }
  :deep(h3) { font-size: 16px; color: #b6f0ff; margin: 8px 0 6px; }
  :deep(h4) { font-size: 14px; color: #c8e6ff; margin: 6px 0 4px; }
  :deep(ul) { padding-left: 22px; margin: 4px 0 8px; }
  :deep(li) { margin: 3px 0; }
  :deep(p)  { margin: 6px 0; }
  :deep(b)  { color: #ffd180; }
}
.ai-meta {
  margin-top: 8px;
  display: flex; gap: 14px; flex-wrap: wrap;
  color: rgba(180,200,230,.7); font-size: 12px;
}
</style>

