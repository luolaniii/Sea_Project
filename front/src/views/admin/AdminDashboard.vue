<template>
  <div class="admin-dashboard">
    <PageHeader title="仪表盘"  />

    <div class="dashboard-grid">
      <div class="stat-card card">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.dataSourceCount || 0 }}</div>
          <div class="stat-label">数据源</div>
        </div>
      </div>

      <div class="stat-card card">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.observationDataCount || 0 }}</div>
          <div class="stat-label">观测数据</div>
        </div>
      </div>

      <div class="stat-card card">
        <div class="stat-icon">🌊</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.sceneCount || 0 }}</div>
          <div class="stat-label">可视化场景</div>
        </div>
      </div>

      <div class="stat-card card">
        <div class="stat-icon">📉</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.chartCount || 0 }}</div>
          <div class="stat-label">图表配置</div>
        </div>
      </div>
    </div>

    <div class="dashboard-content card">
      <h3 class="section-title">快速入口</h3>
      <ul class="feature-list">
        <li>
          <span class="feature-icon">🗄️</span>
          <div>
            <strong>数据源管理</strong>
            <p class="muted">管理NOAA、ERDDAP等数据源配置</p>
          </div>
          <router-link to="/admin/data-source" class="btn btn-primary" style="margin-left: auto">
            进入
          </router-link>
        </li>
        <li>
          <span class="feature-icon">📈</span>
          <div>
            <strong>观测数据</strong>
            <p class="muted">查看和管理海洋观测数据</p>
          </div>
          <router-link to="/admin/observation-data" class="btn btn-primary" style="margin-left: auto">
            进入
          </router-link>
        </li>
        <li>
          <span class="feature-icon">🌊</span>
          <div>
            <strong>场景管理</strong>
            <p class="muted">创建和管理3D可视化场景</p>
          </div>
          <router-link to="/admin/scene" class="btn btn-primary" style="margin-left: auto">
            进入
          </router-link>
        </li>
        <li>
          <span class="feature-icon">📉</span>
          <div>
            <strong>图表配置</strong>
            <p class="muted">配置ECharts图表和数据查询</p>
          </div>
          <router-link to="/admin/chart-config" class="btn btn-primary" style="margin-left: auto">
            进入
          </router-link>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import { dataSourceApi, observationDataApi, visualizationSceneApi, chartConfigApi } from '@/utils/api-admin';

const stats = ref({
  dataSourceCount: 0,
  observationDataCount: 0,
  sceneCount: 0,
  chartCount: 0,
});

const loadStats = async () => {
  try {
    const [dataSourceRes, observationRes, sceneRes, chartRes] = await Promise.all([
      dataSourceApi.page({ pageNum: 1, pageSize: 1 }),
      observationDataApi.page({ pageNum: 1, pageSize: 1 }),
      visualizationSceneApi.page({ pageNum: 1, pageSize: 1 }),
      chartConfigApi.page({ pageNum: 1, pageSize: 1 }),
    ]);

    stats.value = {
      dataSourceCount: dataSourceRes.total,
      observationDataCount: observationRes.total,
      sceneCount: sceneRes.total,
      chartCount: chartRes.total,
    };
  } catch (error) {
    console.error('加载统计数据失败:', error);
  }
};

onMounted(() => {
  loadStats();
});
</script>

<style scoped lang="less">
.admin-dashboard {
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(102, 126, 234, 0.3);
    border-color: rgba(102, 126, 234, 0.5);

    &::before {
      opacity: 1;
    }
  }
}

.stat-icon {
  font-size: 40px;
  filter: drop-shadow(0 2px 8px rgba(102, 126, 234, 0.3));
  position: relative;
  z-index: 1;
}

.stat-content {
  flex: 1;
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  letter-spacing: 0.3px;
}

.dashboard-content {
  margin-top: 32px;
  animation: fadeIn 0.6s ease-out 0.2s both;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid rgba(102, 126, 234, 0.3);
  letter-spacing: 0.3px;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    transform: scaleY(0);
    transition: transform 0.3s;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(102, 126, 234, 0.3);
    transform: translateX(8px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);

    &::before {
      transform: scaleY(1);
    }
  }

  > div {
    flex: 1;
  }
}

.feature-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.feature-list li strong {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 6px;
  letter-spacing: 0.2px;
}

.feature-list li .muted {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  margin: 0;
  line-height: 1.5;
}

// 响应式设计
@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
  }

  .stat-card {
    padding: 20px;
  }

  .stat-icon {
    font-size: 32px;
  }

  .stat-value {
    font-size: 24px;
  }

  .feature-list li {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>

