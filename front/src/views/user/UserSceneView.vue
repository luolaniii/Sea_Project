<template>
  <div class="user-scene-view">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="scene" class="scene-container">
      <div class="scene-header">
        <div class="scene-header-top">
          <button class="btn-back" @click="handleBack">
            <span class="back-icon">←</span>
            <span>返回</span>
          </button>
        </div>
        <h1 class="scene-title">{{ scene.sceneName }}</h1>
        <p class="scene-description">{{ scene.description || '暂无描述' }}</p>
        <div class="scene-meta">
          <span class="scene-type">{{ scene.sceneType }}</span>
          <span class="scene-views"><Icon name="eye" :size="13" /> {{ scene.viewCount || 0 }}</span>
        </div>
      </div>
      <!-- 回放条放在标题区下方、3D 区上方，避免被画布区域 overflow/高度挤压到视口外 -->
      <div v-if="replayRange.valid" class="scene-replay-bar-host">
        <TimeReplayBar
          v-model="replayTimeMs"
          :min-ms="replayRange.min"
          :max-ms="replayRange.max"
        />
      </div>
      <p v-else-if="sceneData.length > 0" class="replay-unavailable-hint">
        当前数据无法解析观测时间，历史回放不可用（请确认接口返回 observationTime 为可解析时间）
      </p>
      <div class="scene-content">
        <div class="scene-viewer-wrap">
          <Scene3DViewer
            :config-json="scene.configJson"
            :data="sceneData"
            :source-label="sourceLabel"
            :replay-end-time="replayRange.valid ? replayEndTimeStr : undefined"
          />
        </div>
      </div>
    </div>
    <div v-else class="error">场景不存在</div>
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

const formatMsForApi = (ms: number): string => {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

console.log('=== UserSceneView 组件脚本开始执行 ===');
console.log('UserSceneView 导入完成');

const route = useRoute();
const router = useRouter();
const scene = ref<any>(null);
const sceneData = ref<any[]>([]);
const loading = ref(true);
const replayTimeMs = ref(0);

// 数据源标签：优先从观测数据中读取 dataSourceName，否则从配置/场景名推断
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

console.log('UserSceneView 变量初始化完成');

// 解析场景 configJson（含 dataQuery 大整数 ID，禁止裸 JSON.parse）
const parseSceneConfigRoot = (configJson?: string): any => {
  if (!configJson) return null;
  try {
    return parseJsonPreservingLongIds(configJson);
  } catch (e) {
    console.error('解析场景配置失败:', e);
    return null;
  }
};

// 加载场景数据
const loadSceneData = async (sceneId: string, configJson?: string) => {
  try {
    console.log('=== 开始加载场景数据 ===');
    console.log('sceneId:', sceneId);
    console.log('configJson:', configJson);
    
    const sceneConfig = parseSceneConfigRoot(configJson);
    console.log('解析后的场景配置:', sceneConfig);
    
    const queryReq: any = {
      pageNum: 1,
      pageSize: 1000, // 默认查询1000条数据用于3D渲染
    };

    // 如果有配置，使用配置中的参数
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

      // 支持 dataQuery.time.mode/defaultHours 形式的相对时间范围
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

    console.log('查询请求参数:', queryReq);
    const result = await userApi.getSceneData(sceneId, queryReq);
    console.log('API返回结果:', result);
    console.log('数据列表:', result.list);
    console.log('数据数量:', result.list?.length || 0);
    return result.list || [];
  } catch (error) {
    console.error('加载场景数据失败:', error);
    return [];
  }
};

const sceneIdFromRoute = (): string => routeParamId(route.params.id);

const loadScene = async () => {
  console.log('=== loadScene 开始执行 ===');
  const sceneId = sceneIdFromRoute();
  console.log('从路由获取的sceneId:', sceneId);

  if (!sceneId) {
    console.warn('sceneId无效，停止加载');
    loading.value = false;
    return;
  }

  try {
    loading.value = true;
    console.log('开始获取场景详情，sceneId:', sceneId);
    const data = await userApi.getSceneById(sceneId);
    console.log('获取到的场景详情:', data);
    scene.value = data;

    // 加载场景数据
    const dataList = await loadSceneData(sceneId, data.configJson);
    console.log('=== 原始场景数据 ===');
    console.log('数据量:', dataList.length);
    console.log('第一条数据:', dataList[0]);
    console.log('所有数据:', dataList);
    
    // 保留原始观测数据结构（包含 dataTypeCode 等关联字段），供 Scene3DViewer 按 layers.dataBindings 精准取数
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
    
    console.log('=== 处理后的场景数据 ===');
    console.log('过滤后数据量:', sceneData.value.length);
    console.log('第一条处理后的数据:', sceneData.value[0]);
    console.log('所有处理后的数据:', sceneData.value);
  } catch (error) {
    console.error('加载场景失败:', error);
    console.error('错误详情:', error);
  } finally {
    loading.value = false;
    console.log('loadScene 执行完成，loading设置为false');
  }
};

onMounted(() => {
  console.log('=== UserSceneView onMounted ===');
  console.log('route.params:', route.params);
  loadScene();
});
</script>

<style scoped lang="less">
.user-scene-view {
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

.scene-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.scene-header {
  padding: 28px 32px;
  background: rgba(15, 20, 45, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 10;
}

.scene-header-top {
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

.scene-title {
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

.scene-description {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 16px;
  line-height: 1.6;
  letter-spacing: 0.2px;
}

.scene-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.scene-type {
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3));
  border-radius: 12px;
  font-weight: 500;
  border: 1px solid rgba(102, 126, 234, 0.4);
}

.scene-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: rgba(10, 14, 39, 0.5);
  min-height: 600px;
  height: calc(100vh - 200px);
}

.scene-viewer-wrap {
  flex: 1;
  min-height: 0;
  position: relative;
}

.scene-replay-bar-host {
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
  .scene-header {
    padding: 20px 24px;
  }

  .scene-replay-bar-host {
    padding: 0 16px;
  }

  .scene-title {
    font-size: 22px;
  }

  .scene-description {
    font-size: 14px;
  }
}
</style>

