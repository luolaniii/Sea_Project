<template>
  <div class="user-chart-gallery">
    <PageHeader title="图表画廊" description="浏览和查看数据可视化图表" />

    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="charts.length === 0" class="text-center">暂无图表</div>
    <div v-else class="chart-grid">
      <div v-for="chart in charts" :key="chart.id" class="chart-card card">
        <div class="chart-header">
          <h3 class="chart-name">{{ chart.chartName }}</h3>
          <span class="chart-type">{{ chart.chartType }}</span>
        </div>
        <div class="chart-preview">
          <div class="chart-placeholder"><Icon name="chart" :size="44" color="#00d4ff" /></div>
          <p class="chart-note">图表预览</p>
        </div>
        <div class="chart-info">
          <p class="chart-description">{{ chart.description || '暂无描述' }}</p>
          <div v-if="chartSourceInfo(chart)" class="chart-source">
            <span class="source-dot"></span>
            <span><Icon name="signal" :size="12" /> {{ chartSourceInfo(chart) }}</span>
          </div>
          <div class="chart-actions">
            <router-link :to="`/user/chart/${chart.id}`" class="btn btn-primary">查看图表</router-link>
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
import { ref, onMounted } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import Icon from '@/components/Icon.vue';
import { userApi, type ChartConfig } from '@/utils/api-user';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';

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

const loadCharts = async () => {
  loading.value = true;
  try {
    const res = await userApi.getPublicCharts({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
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

onMounted(() => {
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 28px;
  margin-bottom: 32px;
}

.chart-card {
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
  }
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 18px;
}

.chart-name {
  font-family: 'Cormorant Garamond', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 500;
  color: @ink-1;
  margin: 0;
  letter-spacing: 0.3px;
}

.chart-type {
  font-size: 10.5px;
  padding: 4px 10px;
  background: transparent;
  border-radius: 0;
  color: @ink-2;
  font-weight: 500;
  border: 1px solid @hairline;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.6px;
  text-transform: uppercase;
}

.chart-preview {
  height: 200px;
  background: @cream-tint;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
  position: relative;
  border-top: 1px solid @hairline;
  border-bottom: 1px solid @hairline;
}

.chart-placeholder {
  margin-bottom: 10px;
  opacity: 0.5;
  animation: float 4s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

.chart-note {
  font-size: 10.5px;
  color: @taupe;
  margin: 0;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  font-family: 'JetBrains Mono', monospace;
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
  background: @cream-tint;
  border: 1px solid @hairline;
  border-radius: 999px;
  font-size: 11px;
  color: @ink-3;
  font-weight: 500;
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

