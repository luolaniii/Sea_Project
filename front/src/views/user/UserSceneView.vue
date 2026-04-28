<template>
  <div class="user-scene-view">
    <div v-if="loading" class="state state-loading">
      <div class="state-card">
        <div class="state-spinner"></div>
        <p>正在加载场景数据…</p>
      </div>
    </div>
    <template v-else-if="scene">
      <header class="scene-header">
        <button class="btn-back" @click="handleBack">
          <span class="back-arrow">←</span>
          <span>返回画廊</span>
        </button>
        <div class="scene-title-row">
          <span class="scene-tag">{{ scene.sceneType }}</span>
          <h1 class="scene-title">{{ scene.sceneName }}</h1>
        </div>
        <p class="scene-description">{{ scene.description || '暂无描述' }}</p>
        <div class="scene-meta">
          <span class="meta-item">
            <Icon name="eye" :size="13" />
            浏览 {{ scene.viewCount || 0 }} 次
          </span>
          <span v-if="sourceLabel" class="meta-item meta-source">
            <span class="source-dot"></span>
            <Icon name="signal" :size="12" />
            {{ sourceLabel }}
          </span>
          <span v-if="sceneData.length" class="meta-item">
            <Icon name="chart" :size="12" />
            {{ sceneData.length }} 条观测点
          </span>
        </div>
        <div v-if="stationCtx" class="scene-jump">
          <span class="meta-item station-type">
            <Icon name="database" :size="12" />
            {{ stationCtx.stationTypeDesc }}
          </span>
          <button class="jump-btn" :disabled="!stationCtx.chartId" @click="goToChart">
            <Icon name="chart" :size="13" /> 图表页
          </button>
          <button class="jump-btn" @click="goToAi">
            <Icon name="brain" :size="13" /> AI 分析
          </button>
          <button class="jump-btn" :disabled="!stationCtx.sceneId" @click="goToSceneGallery">
            <Icon name="scene" :size="13" /> 场景画廊
          </button>
          <button class="jump-btn jump-ghost" :disabled="!stationCtx.officialUrl" @click="openOfficial">
            <Icon name="external" :size="13" /> 站点官网
          </button>
          <button class="jump-btn jump-ghost" :disabled="!stationCtx.historyUrl" @click="openHistory">
            <Icon name="database" :size="13" /> 历史数据
          </button>
          <button class="jump-btn jump-ghost" :disabled="!stationCtx.buoyCamUrl" @click="openBuoyCam">
            <Icon name="eye" :size="13" /> 现场图片
          </button>
        </div>
      </header>

      <!-- 现场图像：BuoyCAM 页面可打开；解析到真实图片时再内联展示 -->
      <aside v-if="stationCtx?.buoyCamUrl" class="buoy-cam-card">
        <div class="cam-header">
          <Icon name="eye" :size="14" :color="'#0284c7'" />
          <strong>现场图像 · BuoyCAM</strong>
          <span class="cam-station">{{ stationCtx.stationId || stationCtx.stationName }}</span>
          <a :href="stationCtx.buoyCamUrl" target="_blank" rel="noopener" class="cam-open">在 NDBC 打开</a>
        </div>
        <div v-if="buoyCam.loading" class="cam-placeholder">正在检查 NDBC 现场图片…</div>
        <img
          v-else-if="buoyCam.available && buoyCam.imageUrl"
          :src="buoyCam.imageUrl"
          :alt="`BuoyCAM ${stationCtx.stationId}`"
          class="cam-image"
          loading="lazy"
          referrerpolicy="no-referrer"
          @error="hideBuoyCamImage"
        />
        <img
          v-else-if="buoyCam.tryDirect && stationCtx?.buoyCamUrl"
          :src="stationCtx.buoyCamUrl"
          :alt="`BuoyCAM ${stationCtx.stationId}`"
          class="cam-image"
          loading="lazy"
          referrerpolicy="no-referrer"
          @load="markDirectBuoyCamLoaded"
          @error="hideDirectBuoyCam"
        />
        <div v-else class="cam-placeholder">
          {{ buoyCam.message || '该站点暂无可内联展示的 BuoyCAM 图片，可在 NDBC 打开查看。' }}
        </div>
        <p class="cam-tip">BuoyCAM 仅在白天定时拍摄，部分站点未安装相机</p>
      </aside>

      <div v-if="replayRange.valid" class="scene-replay-bar-host">
        <TimeReplayBar
          v-model="replayTimeMs"
          :min-ms="replayRange.min"
          :max-ms="replayRange.max"
        />
      </div>
      <p v-else-if="sceneData.length > 0" class="replay-unavailable-hint">
        当前数据无法解析观测时间，历史回放不可用
      </p>

      <section v-if="volatileSegments.length" class="scene-volatility">
        <div class="volatility-title">
          <strong>剧烈变化时间段</strong>
          <span>基于当前回放时间点最近 24 小时，按 异常预警 / 稳定性 / 舒适度 / 短期趋势 分类</span>
        </div>
        <div class="volatility-list">
          <div v-for="seg in volatileSegments" :key="`${seg.typeCode}-${seg.startTime}-${seg.endTime}`" :class="['volatility-item', seg.level]">
            <span class="metric">{{ seg.typeLabel }}</span>
            <span class="time">{{ seg.startTime }} ~ {{ seg.endTime }}</span>
            <span class="delta">变化 {{ seg.delta }}</span>
            <span class="values">{{ seg.fromValue }} → {{ seg.toValue }}</span>
            <span v-for="tag in seg.analyses" :key="tag" :class="['analysis-tag', `tag-${tagSlug(tag)}`]">{{ tag }}</span>
          </div>
        </div>
      </section>

      <section class="scene-stage">
        <Scene3DViewer
          :key="sceneRenderKey"
          :config-json="scene.configJson"
          :data="sceneData"
          :source-label="sourceLabel"
          :replay-end-time="replayRange.valid ? replayEndTimeStr : undefined"
        />
      </section>
    </template>
    <div v-else class="state state-error">
      <div class="state-card">
        <p>场景不存在或已被移除</p>
        <button class="btn-back" @click="handleBack">返回画廊</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Scene3DViewer from '@/components/Scene3DViewer.vue';
import TimeReplayBar from '@/components/TimeReplayBar.vue';
import Icon from '@/components/Icon.vue';
import { userApi } from '@/utils/api-user';
import { routeParamId } from '@/utils/path-id';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';
import { parseObservationTimeToMs } from '@/utils/observation-time';
import { buildVolatileSegmentsFromObservations } from '@/utils/volatile-segments';

const formatMsForApi = (ms: number): string => {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const route = useRoute();
const router = useRouter();
const scene = ref<any>(null);
const sceneData = ref<any[]>([]);
const loading = ref(true);
const replayTimeMs = ref(0);
const sceneRenderKey = computed(() => routeParamId(route.params.id));
const mapStations = ref<any[]>([]);
const mapStationsLoaded = ref(false);

const sourceLabel = computed<string>(() => {
  const list = sceneData.value || [];
  const hit = list.find((d: any) => d && d.dataSourceName);
  if (hit && (hit as any).dataSourceName) return String((hit as any).dataSourceName);
  const stationHit = list.find((d: any) => d && d.stationId);
  if (stationHit && (stationHit as any).stationId) return `站点 ${(stationHit as any).stationId}`;
  return '';
});

const replayRange = computed(() => {
  const list = sceneData.value || [];
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

const sceneDataRecent24h = computed(() => {
  if (!replayRange.value.valid) return sceneData.value;
  const endMs = Math.min(Math.max(replayTimeMs.value, replayRange.value.min), replayRange.value.max);
  const startMs = endMs - 24 * 60 * 60 * 1000;
  return (sceneData.value || []).filter((item: any) => {
    const t = parseObservationTimeToMs(item.observationTime);
    return !Number.isNaN(t) && t >= startMs && t <= endMs;
  });
});

const volatileSegments = computed(() => buildVolatileSegmentsFromObservations(sceneDataRecent24h.value, { maxSegments: 8, maxPerType: 2 }));

const tagSlug = (tag: string): string => {
  switch (tag) {
    case '异常预警': return 'alarm';
    case '稳定性': return 'stability';
    case '舒适度': return 'comfort';
    case '短期趋势': return 'trend';
    default: return 'other';
  }
};

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
  router.push('/user/scene-gallery');
};

const parseSceneConfigRoot = (configJson?: string): any => {
  if (!configJson) return null;
  try {
    return parseJsonPreservingLongIds(configJson);
  } catch (e) {
    console.error('解析场景配置失败:', e);
    return null;
  }
};

const sceneDataSourceId = computed<string>(() => {
  const root = parseSceneConfigRoot(scene.value?.configJson);
  const id = root?.dataQuery?.dataSourceId;
  if (id == null || String(id).trim() === '') return '';
  return String(id).trim();
});

const stationCtx = computed(() => {
  if (!sceneDataSourceId.value) return null;
  const hit = mapStations.value.find((s: any) => String(s?.id) === sceneDataSourceId.value);
  if (!hit) return null;
  const charts = Array.isArray(hit.charts) ? hit.charts : [];
  const scenes = Array.isArray(hit.scenes) ? hit.scenes : [];
  return {
    dataSourceId: String(hit.id),
    stationId: hit.stationId ? String(hit.stationId) : '',
    stationName: hit.name ? String(hit.name) : '',
    stationTypeDesc: hit.stationTypeDesc || hit.sourceType || '未分类站点',
    chartId: charts.length > 0 ? charts[0].id : null,
    sceneId: scenes.length > 0 ? scenes[0].id : null,
    officialUrl: String(hit.officialUrl || hit.apiUrl || '').trim(),
    historyUrl: String(hit.historyUrl || '').trim(),
    buoyCamUrl: String(hit.buoyCamUrl || '').trim(),
  };
});

const buoyCam = ref({
  loading: false,
  available: false,
  tryDirect: false,
  imageUrl: '',
  pageUrl: '',
  message: '',
});

const loadBuoyCamImage = async () => {
  const ctx = stationCtx.value;
  if (!ctx?.dataSourceId || !ctx.buoyCamUrl) {
    buoyCam.value = { loading: false, available: false, tryDirect: false, imageUrl: '', pageUrl: '', message: '' };
    return;
  }
  buoyCam.value = { loading: true, available: false, tryDirect: false, imageUrl: '', pageUrl: ctx.buoyCamUrl, message: '' };
  try {
    const res = await userApi.getBuoyCamImage(ctx.dataSourceId);
    buoyCam.value = {
      loading: false,
      available: !!res?.available && !!res?.imageUrl,
      tryDirect: !res?.available || !res?.imageUrl,
      imageUrl: res?.imageUrl || '',
      pageUrl: res?.pageUrl || ctx.buoyCamUrl,
      message: res?.message || '正在尝试直接加载 NDBC 现场图片…',
    };
  } catch (e) {
    console.warn('解析 BuoyCAM 图片失败:', e);
    buoyCam.value = {
      loading: false,
      available: false,
      tryDirect: true,
      imageUrl: '',
      pageUrl: ctx.buoyCamUrl,
      message: '正在尝试直接加载 NDBC 现场图片…',
    };
  }
};

const hideBuoyCamImage = () => {
  buoyCam.value = {
    ...buoyCam.value,
    available: false,
    tryDirect: true,
    imageUrl: '',
    message: '正在尝试直接加载 NDBC 现场图片…',
  };
};

const markDirectBuoyCamLoaded = () => {
  buoyCam.value = { ...buoyCam.value, loading: false, tryDirect: true, message: '' };
};

const hideDirectBuoyCam = () => {
  buoyCam.value = {
    ...buoyCam.value,
    loading: false,
    available: false,
    tryDirect: false,
    imageUrl: '',
    message: '现场图片无法内联加载，可在 NDBC 打开查看。',
  };
};

watch(() => stationCtx.value?.dataSourceId, () => { loadBuoyCamImage(); });

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

const goToChart = () => {
  if (!stationCtx.value?.chartId) return;
  router.push(`/user/chart/${encodeURIComponent(String(stationCtx.value.chartId))}`);
};

const goToAi = () => {
  if (!stationCtx.value?.dataSourceId) return;
  router.push({ path: '/user/ocean-analysis', query: { dataSourceId: stationCtx.value.dataSourceId } });
};

const goToSceneGallery = () => {
  router.push('/user/scene-gallery');
};

const openOfficial = () => {
  const url = stationCtx.value?.officialUrl;
  if (!url) return;
  window.open(url, '_blank');
};

const openBuoyCam = () => {
  const url = stationCtx.value?.buoyCamUrl;
  if (!url) return;
  window.open(url, '_blank');
};

const openHistory = () => {
  const url = stationCtx.value?.historyUrl;
  if (!url) return;
  window.open(url, '_blank');
};

const loadSceneData = async (sceneId: string, configJson?: string) => {
  try {
    const sceneConfig = parseSceneConfigRoot(configJson);
    const queryReq: any = { pageNum: 1, pageSize: 5000 };

    if (sceneConfig && sceneConfig.dataQuery) {
      const dataQuery = sceneConfig.dataQuery;
      if (dataQuery.dataSourceId) queryReq.dataSourceId = dataQuery.dataSourceId;
      if (dataQuery.dataTypeId) queryReq.dataTypeId = dataQuery.dataTypeId;
      if (dataQuery.startTime) queryReq.startTime = dataQuery.startTime;
      if (dataQuery.endTime) queryReq.endTime = dataQuery.endTime;
      if (dataQuery.minLongitude !== undefined) queryReq.minLongitude = dataQuery.minLongitude;
      if (dataQuery.maxLongitude !== undefined) queryReq.maxLongitude = dataQuery.maxLongitude;
      if (dataQuery.minLatitude !== undefined) queryReq.minLatitude = dataQuery.minLatitude;
      if (dataQuery.maxLatitude !== undefined) queryReq.maxLatitude = dataQuery.maxLatitude;
      if (dataQuery.pageSize) queryReq.pageSize = dataQuery.pageSize;

      if (!queryReq.startTime && !queryReq.endTime && dataQuery.time && dataQuery.time.mode === 'range') {
        const defaultHours = Number(dataQuery.time.defaultHours || 0);
        if (defaultHours > 0) {
          const now = new Date();
          const start = new Date(now.getTime() - defaultHours * 60 * 60 * 1000);
          const format = (d: Date) => {
            const pad = (n: number) => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
              d.getMinutes()
            )}:${pad(d.getSeconds())}`;
          };
          queryReq.startTime = format(start);
          queryReq.endTime = format(now);
        }
      }
    }

    const result = await userApi.getSceneData(sceneId, queryReq);
    return result.list || [];
  } catch (error) {
    console.error('加载场景数据失败:', error);
    return [];
  }
};

const sceneIdFromRoute = (): string => {
  const raw = routeParamId(route.params.id);
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
};

const loadScene = async () => {
  const sceneId = sceneIdFromRoute();
  if (!sceneId) {
    scene.value = null;
    sceneData.value = [];
    loading.value = false;
    return;
  }
  try {
    loading.value = true;
    const data = await userApi.getSceneById(sceneId);
    scene.value = data;
    const dataList = await loadSceneData(sceneId, data.configJson);
    sceneData.value = (dataList || []).map((item: any) => {
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
    console.error('加载场景失败:', error);
    scene.value = null;
    sceneData.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadMapStations();
  loadScene();
});

watch(
  () => route.params.id,
  () => {
    loadScene();
  }
);
</script>

<style scoped lang="less">
@accent: #0284c7;
@accent-soft: #7dd3fc;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@ink-4: #94a3b8;
@hairline: #e2e8f0;
@paper: #ffffff;
@bg-soft: #f4f7fa;

.user-scene-view {
  width: 100%;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: -32px;
  padding: 32px;
  background:
    radial-gradient(circle at 80% -10%, rgba(125, 211, 252, 0.3), transparent 55%),
    radial-gradient(circle at 0% 100%, rgba(2, 132, 199, 0.08), transparent 50%),
    linear-gradient(180deg, #f4f9ff 0%, #f4f7fa 100%);
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.state-card {
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 16px;
  padding: 48px 64px;
  text-align: center;
  color: @ink-2;
  font-size: 14px;
  box-shadow: 0 6px 30px rgba(15, 23, 42, 0.06);

  p { margin: 0 0 16px; }

  .btn-back {
    margin: 0 auto;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 18px;
    background: @accent;
    color: #fff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    transition: all 0.2s;
    &:hover { background: darken(@accent, 5%); }
  }
}

.state-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 3px solid rgba(2, 132, 199, 0.15);
  border-top-color: @accent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.scene-header {
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  padding: 22px 28px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.buoy-cam-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid @hairline;
  border-radius: 14px;
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.05);

  .cam-header {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: @ink-1;
    font-family: 'Inter', sans-serif;

    strong { font-weight: 700; letter-spacing: 0.1px; }
    .cam-station {
      padding: 2px 8px;
      background: rgba(2, 132, 199, 0.08);
      color: @accent;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.4px;
    }
    .cam-open {
      margin-left: auto;
      font-size: 11.5px;
      color: @ink-3;
      text-decoration: none;
      padding: 4px 10px;
      border: 1px solid @hairline;
      border-radius: 999px;
      transition: all 0.18s;
      &:hover { color: @accent; border-color: @accent-soft; background: rgba(2, 132, 199, 0.05); }
    }
  }
  .cam-image {
    width: 100%;
    max-height: 320px;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid @hairline;
    background: #f0f7fd;
  }
  .cam-placeholder {
    min-height: 74px;
    border-radius: 10px;
    border: 1px dashed #bae6fd;
    background: #f0f9ff;
    color: @ink-2;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    font-size: 12.5px;
    text-align: center;
  }
  .cam-tip {
    margin: 0;
    font-size: 11.5px;
    color: @ink-3;
    letter-spacing: 0.2px;
  }
}

.btn-back {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(2, 132, 199, 0.08);
  border: 1px solid rgba(2, 132, 199, 0.2);
  border-radius: 999px;
  color: @accent;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
  font-family: inherit;
  letter-spacing: 0.3px;

  &:hover {
    background: rgba(2, 132, 199, 0.14);
    border-color: rgba(2, 132, 199, 0.35);
    transform: translateX(-2px);
  }
}

.back-arrow {
  font-size: 14px;
  font-weight: 700;
}

.scene-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.scene-tag {
  padding: 3px 10px;
  font-size: 10.5px;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.7px;
  text-transform: uppercase;
  color: @accent;
  background: rgba(2, 132, 199, 0.08);
  border: 1px solid rgba(2, 132, 199, 0.2);
  border-radius: 4px;
}

.scene-title {
  font-size: 24px;
  font-weight: 700;
  color: @ink-1;
  letter-spacing: -0.3px;
  margin: 0;
  line-height: 1.2;
}

.scene-description {
  font-size: 14px;
  color: @ink-3;
  line-height: 1.65;
  margin: 0;
  max-width: 720px;
}

.scene-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: @ink-3;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.4px;
}

.scene-jump {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.station-type {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 999px;
  padding: 6px 12px;
  color: #0c4a6e;
}

.jump-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #dbe8f4;
  background: #f8fbff;
  color: #0f172a;
  border-radius: 9px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:hover:not(:disabled) {
    background: #e0f2fe;
    border-color: #7dd3fc;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.jump-btn.jump-ghost {
  background: #fff;
  color: #475569;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta-source {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid @hairline;
  border-radius: 999px;
}

.source-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
  flex-shrink: 0;
}

.scene-replay-bar-host {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid @hairline;
  border-radius: 12px;
  padding: 10px 18px;
}

.replay-unavailable-hint {
  margin: 0;
  padding: 10px 14px;
  font-size: 12px;
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: 8px;
}

.scene-volatility {
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid @hairline;
  border-radius: 14px;
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.05);
}

.volatility-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;

  strong {
    color: @ink-1;
    font-size: 14px;
  }

  span {
    color: @ink-3;
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
  color: @ink-2;
  font-size: 12px;

  &.high {
    background: #fff7ed;
    border-color: #fed7aa;
  }

  .metric {
    grid-row: span 2;
    align-self: center;
    color: @accent;
    font-weight: 700;
  }

  .time {
    color: @ink-1;
    font-weight: 600;
  }

  .delta,
  .values {
    color: @ink-3;
  }

  .analysis-tag {
    grid-column: 2;
    justify-self: start;
    margin-top: 2px;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.4px;
    border: 1px solid;
    margin-right: 4px;
  }
  .tag-alarm { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }
  .tag-stability { background: #eff6ff; color: #1d4ed8; border-color: #bfdbfe; }
  .tag-comfort { background: #f0fdf4; color: #166534; border-color: #bbf7d0; }
  .tag-trend { background: #fefce8; color: #a16207; border-color: #fde68a; }
}

.scene-stage {
  flex: 1;
  min-height: 600px;
  height: calc(100vh - 320px);
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  border: 1px solid @hairline;
  box-shadow: 0 24px 60px -20px rgba(2, 132, 199, 0.25);
  background: #dceefa;
}

@media (max-width: 768px) {
  .user-scene-view { padding: 20px; margin: -20px; }
  .scene-header { padding: 18px 20px; }
  .scene-title { font-size: 20px; }
  .scene-stage { height: 70vh; min-height: 500px; }
}
</style>
