<template>
  <div class="user-scene-gallery">
    <PageHeader title="场景画廊" description="浏览和体验3D海洋可视化场景" />

    <div class="scene-filter-bar">
      <input
        v-model="filters.stationKeyword"
        class="filter-input"
        type="text"
        placeholder="搜索站点名或站点ID"
        @keyup.enter="handleFilter"
      />
      <select v-model="filters.stationType" class="filter-input" @change="handleFilter">
        <option value="">全部站点类型</option>
        <option v-for="item in stationTypeOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
      <select v-model="filters.oceanRegion" class="filter-input" @change="handleFilter">
        <option value="">全部海域</option>
        <option v-for="item in oceanOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
      <input
        v-model="filters.dataTypeCode"
        class="filter-input"
        type="text"
        placeholder="数据类型，如 WVHT / WTMP"
        @keyup.enter="handleFilter"
      />
      <input v-model.number="filters.minLongitude" class="filter-input" type="number" placeholder="最小经度" />
      <input v-model.number="filters.maxLongitude" class="filter-input" type="number" placeholder="最大经度" />
      <input v-model.number="filters.minLatitude" class="filter-input" type="number" placeholder="最小纬度" />
      <input v-model.number="filters.maxLatitude" class="filter-input" type="number" placeholder="最大纬度" />
      <button class="btn btn-primary" @click="handleFilter">搜索</button>
      <button class="btn btn-default" @click="resetFilters">重置</button>
    </div>

    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="scenes.length === 0" class="text-center">暂无场景</div>
    <div v-else class="scene-grid">
      <div v-for="scene in scenes" :key="scene.id" class="scene-card card">
        <div class="scene-thumbnail">
          <img v-if="scene.thumbnail" :src="scene.thumbnail" :alt="scene.sceneName" />
          <SceneCover v-else :config-json="scene.configJson" />
        </div>
        <div class="scene-info">
          <h3 class="scene-name">{{ scene.sceneName }}</h3>
          <p class="scene-description">{{ scene.description || '暂无描述' }}</p>
          <div class="scene-meta">
            <span class="scene-type">{{ scene.sceneType }}</span>
            <span class="scene-views"><Icon name="eye" :size="12" /> {{ scene.viewCount || 0 }}</span>
          </div>
          <div class="scene-source" v-if="sceneSourceInfo(scene)">
            <span class="source-dot"></span>
            <span class="source-text"><Icon name="signal" :size="12" /> {{ sceneSourceInfo(scene) }}</span>
          </div>
          <div class="scene-actions">
            <router-link :to="sceneViewPath(scene.id)" class="btn btn-primary">查看场景</router-link>
          </div>
        </div>
      </div>
    </div>

    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadScenes"
      @page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import SceneCover from '@/components/SceneCover.vue';
import Icon from '@/components/Icon.vue';
import { userApi, type VisualizationScene } from '@/utils/api-user';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';

/** 从 configJson 中提取数据源简述（Module D：画廊数据源可见性） */
const sceneSourceInfo = (scene: VisualizationScene): string => {
  try {
    if (!scene.configJson) return '';
    const cfg: any = parseJsonPreservingLongIds(scene.configJson);
    const q = cfg?.dataQuery || {};
    if (q.dataSourceName) return String(q.dataSourceName);
    if (q.stationId) return `站点 ${q.stationId}`;
    if (q.dataSourceId != null) return `数据源 #${q.dataSourceId}`;
  } catch {
    // 静默：配置 JSON 格式不正常不阻塞画廊
  }
  return '';
};

const scenes = ref<VisualizationScene[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const mapStations = ref<any[]>([]);
const filters = ref({
  stationKeyword: '',
  stationType: '',
  oceanRegion: '',
  dataTypeCode: '',
  minLongitude: undefined as number | undefined,
  maxLongitude: undefined as number | undefined,
  minLatitude: undefined as number | undefined,
  maxLatitude: undefined as number | undefined,
});

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

const sceneViewPath = (id: string | number | undefined): string => {
  if (id == null || String(id).trim() === '') return '/user/scene-gallery';
  return `/user/scene/${encodeURIComponent(String(id).trim())}`;
};

const loadScenes = async () => {
  loading.value = true;
  try {
    const stationKeyword = filters.value.stationKeyword.trim();
    const res = await userApi.getPublicScenes({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      stationKeyword: stationKeyword || undefined,
      stationType: filters.value.stationType || undefined,
      oceanRegion: filters.value.oceanRegion || undefined,
      dataTypeCode: filters.value.dataTypeCode.trim() || undefined,
      minLongitude: filters.value.minLongitude,
      maxLongitude: filters.value.maxLongitude,
      minLatitude: filters.value.minLatitude,
      maxLatitude: filters.value.maxLatitude,
    });
    scenes.value = res.list;
    total.value = res.total;
  } catch (error) {
    console.error('加载场景失败:', error);
  } finally {
    loading.value = false;
  }
};

const loadMapStations = async () => {
  try {
    mapStations.value = await userApi.getMapStations();
  } catch {
    mapStations.value = [];
  }
};

const handleFilter = () => {
  pageNum.value = 1;
  loadScenes();
};

const resetFilters = () => {
  filters.value = {
    stationKeyword: '',
    stationType: '',
    oceanRegion: '',
    dataTypeCode: '',
    minLongitude: undefined,
    maxLongitude: undefined,
    minLatitude: undefined,
    maxLatitude: undefined,
  };
  handleFilter();
};

const handlePageSizeChange = () => {
  pageNum.value = 1; // 页大小改变时重置到第一页
  loadScenes();
};

onMounted(() => {
  loadMapStations();
  loadScenes();
});
</script>

<style scoped lang="less">
.user-scene-gallery {
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

.scene-filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(4, minmax(150px, 1fr)) repeat(4, minmax(120px, .8fr)) auto auto;
  gap: 10px;
  align-items: center;
  margin: 0 0 18px;
}

.filter-input {
  height: 38px;
  border-radius: 8px;
  border: 1px solid #dbe8f4;
  background: #ffffff;
  color: #0f172a;
  padding: 0 12px;
  font-size: 13px;

  &::placeholder {
    color: #64748b;
  }
}

.scene-source {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 8px;
  padding: 4px 10px;
  background: rgba(2, 132, 199, 0.06);
  border: 1px solid rgba(2, 132, 199, 0.18);
  border-radius: 999px;
  font-size: 11.5px;
  color: @accent;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: 0.2px;
  font-weight: 500;
}
.source-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
  flex-shrink: 0;
}
.source-text {
  font-weight: 500;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 28px;
  margin-bottom: 32px;
}

.scene-card {
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

    .scene-thumbnail img { transform: scale(1.04); }
  }
}

.scene-thumbnail {
  width: 100%;
  height: 220px;
  background: linear-gradient(135deg, #dceefa 0%, #bae0fa 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
}

.scene-info {
  padding: 22px 24px 24px;
}

.scene-name {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: @ink-1;
  margin-bottom: 8px;
  letter-spacing: -0.2px;
  line-height: 1.3;
}

.scene-description {
  font-size: 13px;
  color: @ink-3;
  margin-bottom: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scene-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: @ink-3;
  margin-bottom: 12px;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: 0.1px;
}

.scene-views {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.scene-actions {
  margin-top: 16px;

  .btn {
    width: 100%;
  }
}

.scene-type {
  padding: 3px 10px;
  background: rgba(2, 132, 199, 0.08);
  border-radius: 6px;
  font-weight: 600;
  border: none;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: @accent;
  font-size: 10.5px;
  font-family: 'Inter', sans-serif;
}

// 响应式设计
@media (max-width: 768px) {
  .scene-filter-bar {
    grid-template-columns: 1fr;
  }

  .scene-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .scene-card {
    &:hover {
      transform: translateY(-4px);
    }
  }
}
</style>

