<template>
  <div class="station-map-page">
    <div class="page-header glass">
      <div class="header-left">
        <h2><Icon name="map" :size="22" color="#00d4ff" /> 全球海洋观测站点地图</h2>
        <div class="page-eyebrow">
          <span class="dash"></span>
          <span class="eyebrow-text">GLOBAL · NDBC NETWORK</span>
        </div>
        <p class="subtitle">
          当前筛选 {{ filteredStations.length }} / {{ stations.length }} 个站点
          · {{ filteredWaveCount }} 个有波浪数据
          · 输入站名/站点ID可直接搜索定位
        </p>
        <div class="station-filters">
          <input
            v-model="searchKeyword"
            type="text"
            class="station-search"
            placeholder="搜索站点名称 / 站点ID / 类型 / 海域"
          />
          <select v-model="selectedOcean" class="station-select">
            <option value="">全部海域</option>
            <option v-for="o in oceanOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <select v-model="selectedStationType" class="station-select">
            <option value="">全部数据类型</option>
            <option v-for="t in stationTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <select v-model="selectedFeature" class="station-select">
            <option value="">全部站点特征</option>
            <option value="wave">有波浪数据</option>
            <option value="no-wave">仅气象/其他</option>
            <option value="has-chart">有图表</option>
            <option value="has-scene">有场景</option>
            <option value="auto-sync">自动同步</option>
            <option value="no-coord">无坐标</option>
          </select>
        </div>
      </div>
      <div class="legend">
        <span class="legend-item"><span class="dot wave"></span> 有波浪数据</span>
        <span class="legend-item"><span class="dot no-wave"></span> 仅气象/其他</span>
        <span class="legend-item"><span class="dot empty"></span> 无坐标</span>
      </div>
    </div>

    <div class="map-compare-panel glass">
      <div class="compare-head">
        <div>
          <h3><Icon name="bars" :size="17" color="#00d4ff" /> 当前筛选结果对比</h3>
          <p>按上方海域、数据类型、来源机构和站点特征筛选后，对当前结果中的可定位站点做 24 小时多站点比较。</p>
        </div>
        <button class="compare-btn" :disabled="comparisonLoading || compareCandidateIds.length < 2" @click="compareFilteredStations">
          {{ comparisonLoading ? '对比中...' : `对比 ${compareCandidateIds.length} 个站点` }}
        </button>
      </div>
      <p v-if="comparisonError" class="compare-error">{{ comparisonError }}</p>
      <div v-if="comparisonResult" class="compare-result">
        <div v-for="row in comparisonMetricRows" :key="row.key" class="compare-metric">
          <span>{{ row.label }}</span>
          <strong>{{ fmt(row.stat?.mean) }}</strong>
          <small>范围 {{ fmt(row.stat?.min) }} ~ {{ fmt(row.stat?.max) }}</small>
        </div>
        <div class="compare-anomaly">
          <strong>空间异常</strong>
          <span v-if="!comparisonResult.anomalies?.length">未发现显著偏离群体均值的站点</span>
          <span v-else>{{ comparisonResult.anomalies.length }} 个异常：{{ comparisonAnomalyText }}</span>
        </div>
      </div>
    </div>

    <div class="map-container glass">
      <div ref="mapEl" class="leaflet-map"></div>
      <div class="map-overlay-tip">
        <Icon name="radar" :size="14" color="#00d4ff" />
        <span>暗色海洋底图 · 滚轮缩放 · 拖拽平移 · 单击站点</span>
      </div>
    </div>

    <!-- 无坐标站点列表（兜底） -->
    <div v-if="filteredNoCoordStations.length" class="no-coord-panel glass">
      <h4><Icon name="alert" :size="16" color="#faad14" /> 暂无经纬度的站点 ({{ filteredNoCoordStations.length }})</h4>
      <p class="hint">请在管理端为这些站点配置经度/纬度，或使用"批量导入NDBC站点"自动获取坐标。</p>
      <div class="no-coord-grid">
        <span v-for="s in filteredNoCoordStations" :key="String(s.id)" class="no-coord-tag">
          {{ s.name || s.stationId }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { userApi } from '@/utils/api-user';
import Icon from '@/components/Icon.vue';

interface MapStation {
  id: number | string;
  stationId?: string;
  name?: string;
  description?: string;
  longitude?: number | string | null;
  latitude?: number | string | null;
  sourceType?: string;
  stationType?: string;
  stationTypeDesc?: string;
  apiUrl?: string;
  officialUrl?: string;
  historyUrl?: string;
  buoyCamUrl?: string;
  autoSync?: number;
  hasWave?: boolean;
  fileSuffixes?: string;
  chartIds?: (number | string)[];
  sceneIds?: (number | string)[];
  oceanRegion?: string;
  oceanRegionDesc?: string;
}

const router = useRouter();
const stations = ref<MapStation[]>([]);
const searchKeyword = ref('');
const selectedStationType = ref('');
const selectedFeature = ref('');
const selectedOcean = ref('');
const comparisonLoading = ref(false);
const comparisonError = ref('');
const comparisonResult = ref<any>(null);
const mapEl = ref<HTMLDivElement | null>(null);
let map: L.Map | null = null;
let markersLayer: L.LayerGroup | null = null;

const stationTypeOptions = computed(() => {
  const map = new Map<string, string>();
  stations.value.forEach((s) => {
    const code = String(s.stationType || s.sourceType || '').trim();
    const label = String(s.stationTypeDesc || code).trim();
    if (code) map.set(code, label || code);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
});

const oceanOptions = computed(() => {
  const map = new Map<string, string>();
  stations.value.forEach((s) => {
    const code = String(s.oceanRegion || '').trim();
    const label = String(s.oceanRegionDesc || code).trim();
    if (code) map.set(code, label || code);
  });
  return Array.from(map.entries())
    .map(([value, label]) => ({ value, label }))
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-Hans'));
});

const filteredStations = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return stations.value.filter((s) => {
    if (keyword) {
      const hit = [s.name, s.stationId, s.sourceType, s.stationTypeDesc, s.description, s.oceanRegionDesc]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(keyword));
      if (!hit) return false;
    }

    if (selectedStationType.value) {
      const t = String(s.stationType || s.sourceType || '').trim().toUpperCase();
      if (t !== selectedStationType.value.toUpperCase()) return false;
    }

    if (selectedOcean.value) {
      if (String(s.oceanRegion || '').trim().toUpperCase() !== selectedOcean.value.toUpperCase()) return false;
    }

    switch (selectedFeature.value) {
      case 'wave':
        if (!s.hasWave) return false;
        break;
      case 'no-wave':
        if (s.hasWave) return false;
        break;
      case 'has-chart':
        if (!s.chartIds || s.chartIds.length === 0) return false;
        break;
      case 'has-scene':
        if (!s.sceneIds || s.sceneIds.length === 0) return false;
        break;
      case 'auto-sync':
        if (Number(s.autoSync) !== 1) return false;
        break;
      case 'no-coord':
        if (!(toNum(s.longitude) === null || toNum(s.latitude) === null)) return false;
        break;
      default:
        break;
    }

    return true;
  });
});

const stationsWithCoords = computed(() =>
  filteredStations.value.filter((s) => toNum(s.longitude) !== null && toNum(s.latitude) !== null),
);
const filteredNoCoordStations = computed(() =>
  filteredStations.value.filter((s) => toNum(s.longitude) === null || toNum(s.latitude) === null),
);
const filteredWaveCount = computed(() => filteredStations.value.filter((s) => s.hasWave).length);
const compareCandidateIds = computed(() => stationsWithCoords.value.slice(0, 20).map((s) => String(s.id)));
const comparisonMetricRows = computed(() => {
  const metrics = comparisonResult.value?.metrics || {};
  return [
    { key: 'temperature', label: '温度均值', stat: metrics.temperature },
    { key: 'waveHeight', label: '波高均值', stat: metrics.waveHeight },
    { key: 'windSpeed', label: '风速均值', stat: metrics.windSpeed },
    { key: 'stabilityIndex', label: '稳定性均值', stat: metrics.stabilityIndex },
  ];
});
const comparisonAnomalyText = computed(() =>
  (comparisonResult.value?.anomalies || [])
    .slice(0, 3)
    .map((a: any) => a.sourceName || a.dataSourceId)
    .filter(Boolean)
    .join('、'),
);

function toNum(v: any): number | null {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return isNaN(n) ? null : n;
}

function markerColor(s: MapStation): string {
  if (s.hasWave) return '#00d4ff';
  return '#faad14';
}

function fmt(v: any): string {
  if (v === null || v === undefined || v === '') return '—';
  const n = Number(v);
  if (!Number.isFinite(n)) return String(v);
  return n.toFixed(2);
}

function pathWithId(base: string, id: string | number | null | undefined): string {
  if (id == null || String(id).trim() === '') return base;
  return `${base}/${encodeURIComponent(String(id).trim())}`;
}

function escapeHtml(s: string): string {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function openExternalUrl(raw: string) {
  const url = String(raw || '').trim();
  if (!url) return;
  const normalized = /^https?:\/\//i.test(url) ? url : `https://${url}`;
  window.open(normalized, '_blank', 'noopener,noreferrer');
}

/** 内联 SVG 图标（popup 是 DOM 字符串拼接，无法用 Vue 组件） */
const SVG_ICONS = {
  wave:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12c2 0 2-2 4-2s2 2 4 2 2-2 4-2 2 2 4 2 2-2 4-2"/><path d="M2 18c2 0 2-2 4-2s2 2 4 2 2-2 4-2 2 2 4 2 2-2 4-2"/></svg>',
  chart:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="M7 14l4-4 4 3 5-7"/></svg>',
  brain:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 8V4M12 16v4M8 12H4M16 12h4"/></svg>',
  external:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><path d="M15 3h6v6M10 14L21 3"/></svg>',
  signal:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M16.95 7.05a7 7 0 0 1 0 9.9M7.05 16.95a7 7 0 0 1 0-9.9M19.78 4.22a11 11 0 0 1 0 15.56M4.22 19.78a11 11 0 0 1 0-15.56"/></svg>',
  pin:
    '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>',
  eye:
    '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>',
};

/**
 * 用真实 DOM 节点构建 popup（而不是 HTML 字符串），事件绑定 100% 可靠
 */
function buildPopupNode(s: MapStation): HTMLElement {
  const lon = toNum(s.longitude);
  const lat = toNum(s.latitude);
  const sceneId = s.sceneIds && s.sceneIds.length ? s.sceneIds[0] : null;
  const chartId = s.chartIds && s.chartIds.length ? s.chartIds[0] : null;
  const officialUrl = (s.officialUrl || '').trim();
  const buoyCamUrl = (s.buoyCamUrl || '').trim();
  const historyUrl = (s.historyUrl || '').trim();

  const root = document.createElement('div');
  root.className = 'pp-card';
  L.DomEvent.disableClickPropagation(root);
  L.DomEvent.disableScrollPropagation(root);

  const titleHtml = `<span class="pp-icon">${SVG_ICONS.signal}</span> ${escapeHtml(s.name || s.stationId || '未命名站点')}`;
  const meta =
    `<span class="pp-tag ${s.hasWave ? 'wave' : 'other'}">${s.hasWave ? '波浪数据' : '仅气象'}</span>` +
    `<span class="pp-tag type">${escapeHtml(s.stationTypeDesc || s.sourceType || '未分类')}</span>` +
    (s.oceanRegionDesc ? `<span class="pp-tag ocean">${escapeHtml(s.oceanRegionDesc)}</span>` : '') +
    (Number(s.autoSync) === 1 ? `<span class="pp-tag auto">自动同步</span>` : '') +
    (s.stationId ? `<span class="pp-tag id">${escapeHtml(s.stationId)}</span>` : '');
  const coord =
    lat !== null && lon !== null
      ? `<div class="pp-coord">${SVG_ICONS.pin} <span>${lat.toFixed(3)}°, ${lon.toFixed(3)}°</span></div>`
      : '';
  const desc = s.description ? `<div class="pp-desc">${escapeHtml(s.description)}</div>` : '';

  root.innerHTML = `
    <div class="pp-title">${titleHtml}</div>
    <div class="pp-meta">${meta}</div>
    ${coord}
    ${desc}
    <div class="pp-actions"></div>`;

  const actions = root.querySelector<HTMLDivElement>('.pp-actions')!;

  const mkBtn = (
    iconSvg: string,
    label: string,
    cls: string,
    handler: () => void,
    disabled = false,
  ) => {
    const b = document.createElement('button');
    b.className = `pp-btn ${cls}`;
    b.innerHTML = `<span class="pp-btn-icon">${iconSvg}</span><span>${escapeHtml(label)}</span>`;
    if (disabled) {
      b.disabled = true;
      b.classList.add('disabled');
    } else {
      b.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        handler();
      });
    }
    actions.appendChild(b);
  };

  mkBtn(
    SVG_ICONS.wave,
    sceneId ? '查看 3D 场景' : '暂无场景',
    'primary',
    () => router.push(pathWithId('/user/scene', sceneId)),
    !sceneId,
  );
  mkBtn(
    SVG_ICONS.chart,
    chartId ? '查看图表' : '暂无图表',
    '',
    () => router.push(pathWithId('/user/chart', chartId)),
    !chartId,
  );
  mkBtn(SVG_ICONS.brain, '辅助决策 AI', '', () =>
    router.push({ path: '/user/ocean-analysis', query: { dataSourceId: String(s.id) } }),
  );
  mkBtn(
    SVG_ICONS.external,
    '站点官网',
    'ghost',
    () => openExternalUrl(officialUrl),
    !officialUrl,
  );
  mkBtn(
    SVG_ICONS.eye,
    '现场图片',
    'ghost',
    () => openExternalUrl(buoyCamUrl),
    !buoyCamUrl,
  );
  mkBtn(
    SVG_ICONS.external,
    '历史数据',
    'ghost',
    () => openExternalUrl(historyUrl),
    !historyUrl,
  );

  return root;
}

async function loadStations() {
  try {
    const list = (await userApi.getMapStations()) as any[];
    stations.value = (list || []).map((s) => {
      const charts: any[] = Array.isArray(s.charts) ? s.charts : [];
      const scenes: any[] = Array.isArray(s.scenes) ? s.scenes : [];
      const suffix: string = (s.fileSuffixes || '').toLowerCase();
      return {
        id: s.id,
        stationId: s.stationId,
        name: s.name,
        description: s.description,
        sourceType: s.sourceType,
        stationType: s.stationType,
        stationTypeDesc: s.stationTypeDesc,
        apiUrl: s.apiUrl,
        officialUrl: s.officialUrl,
        historyUrl: s.historyUrl,
        buoyCamUrl: s.buoyCamUrl,
        autoSync: Number(s.autoSync ?? 0) || 0,
        longitude: s.longitude,
        latitude: s.latitude,
        fileSuffixes: s.fileSuffixes,
        chartIds: charts.map((c) => c.id),
        sceneIds: scenes.map((sc) => sc.id),
        hasWave: Boolean(s.hasWaveData) || suffix.includes('spec') || suffix.includes('wave') || charts.length > 0,
        oceanRegion: s.oceanRegion,
        oceanRegionDesc: s.oceanRegionDesc,
      } as MapStation;
    });
    await nextTick();
    renderMarkers();
  } catch (e) {
    console.error('加载站点地图失败', e);
  }
}

async function compareFilteredStations() {
  if (compareCandidateIds.value.length < 2) {
    comparisonError.value = '当前筛选结果不足 2 个可定位站点，无法对比';
    comparisonResult.value = null;
    return;
  }
  comparisonLoading.value = true;
  comparisonError.value = '';
  try {
    comparisonResult.value = await userApi.analyzeOceanCompare({
      dataSourceIds: compareCandidateIds.value,
      historyHours: 24,
    });
  } catch (e: any) {
    comparisonError.value = e?.message || '多站点对比失败';
    comparisonResult.value = null;
  } finally {
    comparisonLoading.value = false;
  }
}

function initMap() {
  if (!mapEl.value || map) return;
  map = L.map(mapEl.value, {
    center: [20, 0],
    zoom: 2,
    minZoom: 2,
    maxZoom: 12,
    worldCopyJump: true,
    preferCanvas: true,
    zoomControl: true,
    attributionControl: true,
  });

  // 暗色海洋风底图 (CartoDB Dark Matter)
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    subdomains: 'abcd',
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/">CARTO</a>',
  }).addTo(map);

  markersLayer = L.layerGroup().addTo(map);
}

function renderMarkers() {
  if (!map || !markersLayer) return;
  markersLayer.clearLayers();

  stationsWithCoords.value.forEach((s) => {
    const lat = toNum(s.latitude)!;
    const lon = toNum(s.longitude)!;
    const color = markerColor(s);

    const marker = L.circleMarker([lat, lon], {
      radius: 7,
      color: color,
      weight: 2,
      fillColor: color,
      fillOpacity: 0.85,
      className: 'glow-marker',
    }).addTo(markersLayer!);

    // 脉冲外圈：interactive=false 避免拦截点击
    const halo = L.circleMarker([lat, lon], {
      radius: 14,
      color: color,
      weight: 1,
      fill: false,
      opacity: 0.4,
      className: 'pulse-halo',
      interactive: false,
      bubblingMouseEvents: false,
    }).addTo(markersLayer!);
    void halo;

    marker.bindPopup(buildPopupNode(s), {
      maxWidth: 340,
      className: 'ocean-popup',
      closeButton: true,
      autoPan: true,
    });
    // 双保险：点击 marker 强制开 popup（防 leaflet 内部异常静默吃掉点击）
    marker.on('click', (ev) => {
      L.DomEvent.stopPropagation(ev);
      marker.openPopup();
    });
  });

  // 自适应视野
  if (stationsWithCoords.value.length > 0) {
    const bounds = L.latLngBounds(
      stationsWithCoords.value.map((s) => [toNum(s.latitude)!, toNum(s.longitude)!] as L.LatLngTuple),
    );
    map.fitBounds(bounds.pad(0.2), { maxZoom: 5 });
  }
}

onMounted(async () => {
  initMap();
  await loadStations();
});

watch([searchKeyword, selectedStationType, selectedFeature, selectedOcean], () => {
  comparisonResult.value = null;
  comparisonError.value = '';
  renderMarkers();
});

onUnmounted(() => {
  if (map) {
    map.remove();
    map = null;
  }
});
</script>

<style lang="less">
.station-map-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px;
}

.glass {
  background: linear-gradient(135deg, rgba(20, 40, 70, 0.55), rgba(10, 26, 46, 0.65));
  backdrop-filter: blur(16px);
  border: 1px solid rgba(74, 144, 226, 0.25);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 28px;
  flex-wrap: wrap;
  gap: 12px;

  .page-eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    margin-top: 6px;
    margin-bottom: 0;
  }
  .page-eyebrow .dash {
    width: 28px;
    height: 1px;
    background: linear-gradient(90deg, transparent, #00d4ff);
  }
  .page-eyebrow .eyebrow-text {
    font-family: 'JetBrains Mono', monospace;
    font-size: 11px;
    letter-spacing: 2.4px;
    color: rgba(0, 212, 255, 0.7);
  }

  h2 {
    margin: 0;
    color: #e0f2ff;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.4px;
    display: inline-flex;
    align-items: center;
    gap: 10px;
  }
  .subtitle {
    margin: 8px 0 0;
    color: rgba(224, 242, 255, 0.55);
    font-size: 13px;
    letter-spacing: 0.3px;
  }

  .station-filters {
    margin-top: 12px;
    display: grid;
    grid-template-columns: minmax(260px, 1.4fr) repeat(3, minmax(160px, 1fr));
    gap: 10px;
    width: min(1040px, 100%);
  }

  .station-search,
  .station-select {
    height: 36px;
    border-radius: 10px;
    border: 1px solid rgba(125, 211, 252, 0.32);
    background: rgba(255, 255, 255, 0.94);
    color: #0f172a;
    padding: 0 12px;
    font-size: 13px;
    letter-spacing: 0.2px;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:focus {
      outline: none;
      border-color: rgba(0, 212, 255, 0.62);
      box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.15);
    }
  }

  .station-search::placeholder {
    color: #64748b;
  }
}

.legend {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(224, 242, 255, 0.85);

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    box-shadow: 0 0 8px currentColor;
  }
  .dot.wave {
    background: #00d4ff;
    color: #00d4ff;
  }
  .dot.no-wave {
    background: #faad14;
    color: #faad14;
  }
  .dot.empty {
    background: #999;
    color: #999;
  }
}

.map-compare-panel {
  padding: 16px 20px;
  color: rgba(224, 242, 255, 0.9);
}

.compare-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  h3 {
    margin: 0 0 6px;
    color: #e0f2ff;
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 0.3px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  p {
    margin: 0;
    color: rgba(224, 242, 255, 0.68);
    font-size: 12.5px;
    line-height: 1.6;
  }
}

.compare-btn {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #02a8df, #22d3ee);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.2px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(2, 132, 199, 0.24);
  transition: transform 0.2s ease, opacity 0.2s ease, box-shadow 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 14px 28px rgba(2, 132, 199, 0.32);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.compare-error {
  margin: 12px 0 0;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.32);
  color: #fecaca;
  font-size: 12px;
}

.compare-result {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(155px, 1fr));
  gap: 10px;
}

.compare-metric,
.compare-anomaly {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(125, 211, 252, 0.26);
}

.compare-metric {
  span {
    color: rgba(224, 242, 255, 0.68);
    font-size: 12px;
  }

  strong {
    display: block;
    margin: 5px 0 3px;
    color: #ffffff;
    font-size: 22px;
    line-height: 1.1;
  }

  small {
    color: rgba(224, 242, 255, 0.58);
    font-size: 11px;
  }
}

.compare-anomaly {
  grid-column: span 2;
  display: flex;
  flex-direction: column;
  gap: 5px;

  strong {
    color: #ffffff;
    font-size: 13px;
  }

  span {
    color: rgba(224, 242, 255, 0.72);
    font-size: 12px;
    line-height: 1.55;
  }
}

.map-container {
  position: relative;
  height: calc(100vh - 240px);
  min-height: 520px;
  overflow: hidden;
}

.leaflet-map {
  width: 100%;
  height: 100%;
  border-radius: 14px;
}

.map-overlay-tip {
  position: absolute;
  bottom: 14px;
  left: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.9);
  color: #0c4a6e;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.3px;
  border: 1px solid rgba(2, 132, 199, 0.22);
  z-index: 500;
  pointer-events: none;
  backdrop-filter: blur(8px);
}

// 自定义 Leaflet popup
.leaflet-popup.ocean-popup .leaflet-popup-content-wrapper {
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  border: 1px solid rgba(2, 132, 199, 0.26);
  border-radius: 12px;
  box-shadow: 0 18px 34px -14px rgba(2, 132, 199, 0.45);
  backdrop-filter: blur(12px);
}
.leaflet-popup.ocean-popup .leaflet-popup-tip {
  background: rgba(255, 255, 255, 0.96);
}
.leaflet-popup.ocean-popup .leaflet-popup-close-button {
  color: #0284c7;
  font-size: 22px;
}

.pp-card {
  min-width: 240px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
.pp-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 10px;
  letter-spacing: 0.3px;
}
.pp-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
  color: #0284c7;
}
.pp-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.pp-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.3px;
  background: #f1f5f9;
  border: 1px solid #dbe8f4;
  color: #475569;
}
.pp-tag.wave {
  background: #dff6ff;
  color: #0369a1;
  border-color: #a5e6ff;
}
.pp-tag.type {
  background: #ecfeff;
  color: #0c4a6e;
  border-color: #a5f3fc;
}
.pp-tag.auto {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
}
.pp-tag.other {
  background: #fef3c7;
  color: #b45309;
  border-color: #fde68a;
}
.pp-tag.id {
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.6px;
}
.pp-tag.ocean {
  background: #eef2ff;
  color: #4338ca;
  border-color: #c7d2fe;
}
.pp-coord {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
}
.pp-desc {
  font-size: 12px;
  color: #475569;
  margin-bottom: 12px;
  line-height: 1.55;
  max-height: 60px;
  overflow: hidden;
}
.pp-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pp-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f8fbff;
  color: #0f172a;
  border: 1px solid #dbe8f4;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  letter-spacing: 0.3px;
  text-align: left;
  transition: all 0.22s ease;
  font-family: inherit;
}
.pp-btn .pp-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #0284c7;
}
.pp-btn:hover:not(.disabled):not(:disabled) {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0f172a;
  transform: translateX(1px);
}
.pp-btn.primary {
  background: linear-gradient(135deg, #0284c7, #06b6d4);
  border-color: rgba(2, 132, 199, 0.45);
  color: #fff;
}
.pp-btn.ghost {
  background: transparent;
  border-color: #cbd5e1;
  color: #64748b;
}
.pp-btn.ghost .pp-btn-icon {
  color: #64748b;
}
.pp-btn.disabled,
.pp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

// 站点闪烁动画
@keyframes haloPulse {
  0% { transform: scale(0.6); opacity: 0.7; }
  100% { transform: scale(1.6); opacity: 0; }
}
.leaflet-interactive.pulse-halo {
  animation: haloPulse 2s ease-out infinite;
  transform-origin: center;
}

.no-coord-panel {
  padding: 18px 22px;

  h4 {
    margin: 0 0 6px;
    color: #ffc868;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 0.3px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  .hint {
    margin: 0 0 10px;
    color: rgba(224, 242, 255, 0.55);
    font-size: 12px;
    letter-spacing: 0.3px;
  }
  .no-coord-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .no-coord-tag {
    display: inline-block;
    background: rgba(250, 173, 20, 0.15);
    color: #ffc868;
    border: 1px solid rgba(250, 173, 20, 0.3);
    padding: 3px 10px;
    border-radius: 12px;
    font-size: 12px;
  }
}

@media (max-width: 980px) {
  .page-header .station-filters {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .compare-anomaly {
    grid-column: auto;
  }
}
</style>
