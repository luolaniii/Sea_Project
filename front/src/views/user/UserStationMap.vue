<template>
  <div class="station-map-page">
    <div class="page-header glass">
      <div class="header-left">
        <div class="page-eyebrow">
          <span class="dash"></span>
          <span class="eyebrow-text">GLOBAL · NDBC NETWORK</span>
        </div>
        <h2><Icon name="map" :size="22" color="#00d4ff" /> 全球海洋观测站点地图</h2>
        <p class="subtitle">{{ stations.length }} 个站点 · {{ waveCount }} 个有波浪数据 · 点击站点跳转可视化</p>
      </div>
      <div class="legend">
        <span class="legend-item"><span class="dot wave"></span> 有波浪数据</span>
        <span class="legend-item"><span class="dot no-wave"></span> 仅气象/其他</span>
        <span class="legend-item"><span class="dot empty"></span> 无坐标</span>
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
    <div v-if="noCoordStations.length" class="no-coord-panel glass">
      <h4><Icon name="alert" :size="16" color="#faad14" /> 暂无经纬度的站点 ({{ noCoordStations.length }})</h4>
      <p class="hint">请在管理端为这些站点配置经度/纬度，或使用"批量导入NDBC站点"自动获取坐标。</p>
      <div class="no-coord-grid">
        <span v-for="s in noCoordStations" :key="String(s.id)" class="no-coord-tag">
          {{ s.name || s.stationId }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed, nextTick } from 'vue';
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
  hasWave?: boolean;
  fileSuffixes?: string;
  chartIds?: (number | string)[];
  sceneIds?: (number | string)[];
}

const router = useRouter();
const stations = ref<MapStation[]>([]);
const mapEl = ref<HTMLDivElement | null>(null);
let map: L.Map | null = null;
let markersLayer: L.LayerGroup | null = null;

const stationsWithCoords = computed(() =>
  stations.value.filter((s) => toNum(s.longitude) !== null && toNum(s.latitude) !== null),
);
const noCoordStations = computed(() =>
  stations.value.filter((s) => toNum(s.longitude) === null || toNum(s.latitude) === null),
);
const waveCount = computed(() => stations.value.filter((s) => s.hasWave).length);

function toNum(v: any): number | null {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return isNaN(n) ? null : n;
}

function markerColor(s: MapStation): string {
  if (s.hasWave) return '#00d4ff';
  return '#faad14';
}

function escapeHtml(s: string): string {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
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
};

/**
 * 用真实 DOM 节点构建 popup（而不是 HTML 字符串），事件绑定 100% 可靠
 */
function buildPopupNode(s: MapStation): HTMLElement {
  const lon = toNum(s.longitude);
  const lat = toNum(s.latitude);
  const sceneId = s.sceneIds && s.sceneIds.length ? s.sceneIds[0] : null;
  const chartId = s.chartIds && s.chartIds.length ? s.chartIds[0] : null;

  const root = document.createElement('div');
  root.className = 'pp-card';

  const titleHtml = `<span class="pp-icon">${SVG_ICONS.signal}</span> ${escapeHtml(s.name || s.stationId || '未命名站点')}`;
  const meta =
    `<span class="pp-tag ${s.hasWave ? 'wave' : 'other'}">${s.hasWave ? '波浪数据' : '仅气象'}</span>` +
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
    () => router.push(`/user/scene/${sceneId}`),
    !sceneId,
  );
  mkBtn(
    SVG_ICONS.chart,
    chartId ? '查看图表' : '暂无图表',
    '',
    () => router.push(`/user/chart/${chartId}`),
    !chartId,
  );
  mkBtn(SVG_ICONS.brain, '辅助决策 AI', '', () =>
    router.push({ path: '/user/ocean-analysis', query: { dataSourceId: String(s.id) } }),
  );
  mkBtn(
    SVG_ICONS.external,
    'NDBC 官网',
    'ghost',
    () =>
      window.open(`https://www.ndbc.noaa.gov/station_page.php?station=${s.stationId || ''}`, '_blank'),
    !s.stationId,
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
        longitude: s.longitude,
        latitude: s.latitude,
        fileSuffixes: s.fileSuffixes,
        chartIds: charts.map((c) => c.id),
        sceneIds: scenes.map((sc) => sc.id),
        hasWave: suffix.includes('spec') || charts.length > 0,
      } as MapStation;
    });
    await nextTick();
    renderMarkers();
  } catch (e) {
    console.error('加载站点地图失败', e);
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
    margin-bottom: 6px;
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
    margin: 6px 0 0;
    color: rgba(224, 242, 255, 0.55);
    font-size: 13px;
    letter-spacing: 0.3px;
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
  background: rgba(10, 26, 46, 0.72);
  color: #7ecfff;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.3px;
  border: 1px solid rgba(74, 144, 226, 0.3);
  z-index: 500;
  pointer-events: none;
  backdrop-filter: blur(8px);
}

// 自定义 Leaflet popup
.leaflet-popup.ocean-popup .leaflet-popup-content-wrapper {
  background: linear-gradient(135deg, rgba(15, 35, 60, 0.95), rgba(10, 25, 45, 0.95));
  color: #e0f2ff;
  border: 1px solid rgba(74, 144, 226, 0.4);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.6);
}
.leaflet-popup.ocean-popup .leaflet-popup-tip {
  background: rgba(15, 35, 60, 0.95);
}
.leaflet-popup.ocean-popup .leaflet-popup-close-button {
  color: #7ecfff;
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
  color: #00d4ff;
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
  background: rgba(0, 212, 255, 0.12);
  border: 1px solid rgba(0, 212, 255, 0.3);
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
  background: rgba(74, 144, 226, 0.18);
  border: 1px solid rgba(74, 144, 226, 0.3);
  color: #7ecfff;
}
.pp-tag.wave {
  background: rgba(0, 212, 255, 0.16);
  color: #00d4ff;
  border-color: rgba(0, 212, 255, 0.4);
}
.pp-tag.other {
  background: rgba(250, 173, 20, 0.16);
  color: #ffc868;
  border-color: rgba(250, 173, 20, 0.4);
}
.pp-tag.id {
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.6px;
}
.pp-coord {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(224, 242, 255, 0.7);
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
}
.pp-desc {
  font-size: 12px;
  color: rgba(224, 242, 255, 0.6);
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
  background: rgba(74, 144, 226, 0.1);
  color: rgba(224, 242, 255, 0.9);
  border: 1px solid rgba(74, 144, 226, 0.3);
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
  color: #00d4ff;
}
.pp-btn:hover:not(.disabled):not(:disabled) {
  background: rgba(0, 212, 255, 0.16);
  border-color: rgba(0, 212, 255, 0.55);
  color: #fff;
  transform: translateX(2px);
}
.pp-btn.primary {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.22), rgba(74, 144, 226, 0.18));
  border-color: rgba(0, 212, 255, 0.5);
  color: #fff;
}
.pp-btn.ghost {
  background: transparent;
  border-color: rgba(224, 242, 255, 0.18);
  color: rgba(224, 242, 255, 0.65);
}
.pp-btn.ghost .pp-btn-icon {
  color: rgba(224, 242, 255, 0.6);
}
.pp-btn.disabled,
.pp-btn:disabled {
  opacity: 0.4;
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
</style>
