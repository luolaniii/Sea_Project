<template>
  <div class="user-scene-gallery">
    <PageHeader title="场景画廊" description="浏览和体验3D海洋可视化场景" />

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
            <router-link :to="`/user/scene/${scene.id}`" class="btn btn-primary">查看场景</router-link>
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
import { ref, onMounted } from 'vue';
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

const loadScenes = async () => {
  loading.value = true;
  try {
    const res = await userApi.getPublicScenes({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    scenes.value = res.list;
    total.value = res.total;
  } catch (error) {
    console.error('加载场景失败:', error);
  } finally {
    loading.value = false;
  }
};

const handlePageSizeChange = () => {
  pageNum.value = 1; // 页大小改变时重置到第一页
  loadScenes();
};

onMounted(() => {
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

@hairline: #d8cebd;
@taupe-soft: #c2b8a6;
@taupe: #8d8576;
@paper: #fbf7ee;
@cream-tint: #e8e0cf;
@ink-1: #1c1814;
@ink-2: #3a342c;
@ink-3: #6e6759;

.text-center {
  padding: 80px 20px;
  color: @ink-3;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.scene-source {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 8px;
  padding: 4px 10px;
  background: @cream-tint;
  border: 1px solid @hairline;
  border-radius: 999px;
  font-size: 11px;
  color: @ink-3;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.4px;
}
.source-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6f8a6a;
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
  transition: transform 0.32s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.32s cubic-bezier(0.2, 0.8, 0.2, 1),
              border-color 0.3s ease;
  position: relative;
  cursor: pointer;

  &:hover {
    transform: translateY(-6px);
    border-color: @taupe-soft;
    box-shadow: 0 24px 48px -16px rgba(60, 50, 40, 0.18);

    .scene-thumbnail img { transform: scale(1.04); }
  }
}

.scene-thumbnail {
  width: 100%;
  height: 220px;
  background: #1a1612;
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
  font-family: 'Cormorant Garamond', 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 500;
  color: @ink-1;
  margin-bottom: 8px;
  letter-spacing: 0.3px;
  line-height: 1.25;
}

.scene-description {
  font-size: 13px;
  color: @ink-3;
  margin-bottom: 14px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scene-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: @ink-3;
  margin-bottom: 12px;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.4px;
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
  padding: 4px 10px;
  background: transparent;
  border-radius: 0;
  font-weight: 500;
  border: 1px solid @hairline;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: @ink-2;
  font-size: 10.5px;
}

// 响应式设计
@media (max-width: 768px) {
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

