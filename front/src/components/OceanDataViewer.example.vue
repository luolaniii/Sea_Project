<template>
  <div class="example-container">
    <h2>OceanDataViewer 使用示例</h2>
    
    <!-- 示例1: 从后端接口获取数据，自动识别类型 -->
    <div class="example-section">
      <h3>示例1: 从场景获取数据（自动识别类型）</h3>
      <div v-if="loading1" class="loading">加载中...</div>
      <OceanDataViewer
        v-else
        :data="sceneData"
        :region="computedRegion1"
      />
    </div>

    <!-- 示例2: 从图表获取数据，自动识别类型 -->
    <div class="example-section">
      <h3>示例2: 从图表获取数据（自动识别类型）</h3>
      <div v-if="loading2" class="loading">加载中...</div>
      <OceanDataViewer
        v-else
        :data="chartData"
        :region="computedRegion2"
      />
    </div>

    <!-- 示例3: 手动指定数据类型代码 -->
    <div class="example-section">
      <h3>示例3: 手动指定数据类型代码</h3>
      <OceanDataViewer
        :data="mockData"
        data-type-code="TEMP"
        data-type-name="海水温度"
      />
    </div>

    <!-- 示例4: 强制指定可视化类型 -->
    <div class="example-section">
      <h3>示例4: 强制指定使用3D地形可视化</h3>
      <OceanDataViewer
        :data="mockData"
        data-type-code="TEMP"
        force-viewer-type="bathymetry"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import OceanDataViewer from './OceanDataViewer.vue';
import { userApi } from '@/utils/api-user';
import type { ObservationData } from '@/utils/api-user';

// 示例1: 从场景获取数据
const sceneData = ref<ObservationData[]>([]);
const loading1 = ref(false);

// 示例2: 从图表获取数据
const chartData = ref<ObservationData[]>([]);
const loading2 = ref(false);

// 示例3: 模拟数据（后端返回格式）
const mockData = ref<ObservationData[]>([
  {
    id: 1,
    dataSourceId: 1,
    dataTypeId: 1,
    observationTime: '2024-01-01T00:00:00',
    dataValue: 25.5,
    longitude: 110.5,
    latitude: 20.3,
    qualityFlag: 'GOOD',
    dataTypeCode: 'TEMP',
    dataTypeName: '海水温度',
    dataTypeUnit: '°C',
  },
  {
    id: 2,
    dataSourceId: 1,
    dataTypeId: 1,
    observationTime: '2024-01-01T01:00:00',
    dataValue: 26.2,
    longitude: 115.2,
    latitude: 25.1,
    qualityFlag: 'GOOD',
    dataTypeCode: 'TEMP',
    dataTypeName: '海水温度',
    dataTypeUnit: '°C',
  },
  {
    id: 3,
    dataSourceId: 1,
    dataTypeId: 1,
    observationTime: '2024-01-01T02:00:00',
    dataValue: 27.8,
    longitude: 120.1,
    latitude: 30.5,
    qualityFlag: 'GOOD',
    dataTypeCode: 'TEMP',
    dataTypeName: '海水温度',
    dataTypeUnit: '°C',
  },
]);

// 计算区域范围
const computedRegion1 = computed(() => {
  if (!sceneData.value || sceneData.value.length === 0) {
    return undefined;
  }
  const longitudes = sceneData.value.map((item) => item.longitude).filter((v) => v != null) as number[];
  const latitudes = sceneData.value.map((item) => item.latitude).filter((v) => v != null) as number[];
  if (longitudes.length === 0 || latitudes.length === 0) {
    return undefined;
  }
  return {
    minLon: Math.min(...longitudes),
    maxLon: Math.max(...longitudes),
    minLat: Math.min(...latitudes),
    maxLat: Math.max(...latitudes),
  };
});

const computedRegion2 = computed(() => {
  if (!chartData.value || chartData.value.length === 0) {
    return undefined;
  }
  const longitudes = chartData.value.map((item) => item.longitude).filter((v) => v != null) as number[];
  const latitudes = chartData.value.map((item) => item.latitude).filter((v) => v != null) as number[];
  if (longitudes.length === 0 || latitudes.length === 0) {
    return undefined;
  }
  return {
    minLon: Math.min(...longitudes),
    maxLon: Math.max(...longitudes),
    minLat: Math.min(...latitudes),
    maxLat: Math.max(...latitudes),
  };
});

// 加载场景数据示例（需要实际的场景ID）
const loadSceneData = async () => {
  try {
    loading1.value = true;
    // 示例：从场景ID为1的场景获取数据
    // const result = await userApi.getSceneData(1, {
    //   pageNum: 1,
    //   pageSize: 1000,
    // });
    // sceneData.value = result.list || [];
    
    // 这里使用模拟数据
    sceneData.value = mockData.value;
  } catch (error) {
    console.error('加载场景数据失败:', error);
  } finally {
    loading1.value = false;
  }
};

// 加载图表数据示例（需要实际的图表ID）
const loadChartData = async () => {
  try {
    loading2.value = true;
    // 示例：从图表ID为1的图表获取数据
    // const result = await userApi.getChartData(1, {
    //   pageNum: 1,
    //   pageSize: 1000,
    // });
    // chartData.value = result.list || [];
    
    // 这里使用模拟数据
    chartData.value = mockData.value;
  } catch (error) {
    console.error('加载图表数据失败:', error);
  } finally {
    loading2.value = false;
  }
};

onMounted(() => {
  loadSceneData();
  loadChartData();
});
</script>

<style scoped lang="less">
.example-container {
  padding: 20px;
}

.example-section {
  margin-bottom: 40px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;

  h3 {
    margin-bottom: 20px;
    color: #fff;
  }
}
</style>

