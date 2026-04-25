<template>
  <div class="ocean-data-viewer">
    <!-- 根据数据类型自动选择对应的可视化组件 -->
    <SSTHeatmapViewer
      v-if="viewerType === 'sst'"
      :data="processedData"
      :region="computedRegion"
      :config="config"
    />
    <ChlorophyllViewer
      v-else-if="viewerType === 'chlorophyll'"
      :data="processedData"
      :region="computedRegion"
      :config="config"
    />
    <Bathymetry3DViewer
      v-else-if="viewerType === 'bathymetry'"
      :data="processedData"
      :region="computedRegion"
      :config="config"
    />
    <Chart2DViewer
      v-else
      :data="processedData"
      :chart-type="fallbackChartType"
      :echarts-config="config?.echartsConfig"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import SSTHeatmapViewer from './SSTHeatmapViewer.vue';
import ChlorophyllViewer from './ChlorophyllViewer.vue';
import Bathymetry3DViewer from './Bathymetry3DViewer.vue';
import Chart2DViewer from './Chart2DViewer.vue';

/**
 * 观测数据接口（与后端返回格式一致）
 */
interface ObservationData {
  id?: number;
  dataSourceId?: string | number;
  dataTypeId?: string | number;
  observationTime?: string;
  dataValue?: number;
  longitude?: number;
  latitude?: number;
  depth?: number;
  qualityFlag?: string;
  // 后端返回的关联数据
  dataSourceName?: string;
  dataTypeName?: string;
  dataTypeCode?: string;
  dataTypeUnit?: string;
  qualityFlagDesc?: string;
  dataSourceTypeDesc?: string;
}

interface Props {
  /**
   * 观测数据列表（后端返回的ObservationData格式）
   */
  data?: ObservationData[];
  /**
   * 数据类型代码（如：TEMP, CHL, SAL等）
   * 如果不提供，会从数据的第一项中获取 dataTypeCode
   */
  dataTypeCode?: string;
  /**
   * 数据类型名称（如：海水温度、叶绿素浓度等）
   * 如果不提供，会从数据的第一项中获取 dataTypeName
   */
  dataTypeName?: string;
  /**
   * 数据类型ID
   */
  dataTypeId?: string | number;
  /**
   * 区域范围（如果不提供，会根据数据自动计算）
   */
  region?: {
    minLon: number;
    maxLon: number;
    minLat: number;
    maxLat: number;
  };
  /**
   * 配置信息
   */
  config?: any;
  /**
   * 强制指定可视化类型（可选，如果不指定则根据数据类型自动判断）
   */
  forceViewerType?: 'sst' | 'chlorophyll' | 'bathymetry' | 'chart';
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
});

/**
 * 从数据中提取数据类型信息
 */
const extractedDataTypeCode = computed(() => {
  if (props.data && props.data.length > 0) {
    return props.data[0].dataTypeCode;
  }
  return undefined;
});

const extractedDataTypeName = computed(() => {
  if (props.data && props.data.length > 0) {
    return props.data[0].dataTypeName;
  }
  return undefined;
});

/**
 * 根据数据类型代码和名称判断应该使用哪个可视化组件
 */
const viewerType = computed(() => {
  // 如果强制指定了类型，直接使用
  if (props.forceViewerType) {
    return props.forceViewerType;
  }

  // 优先使用props传入的，否则从数据中提取
  const typeCode = (props.dataTypeCode || extractedDataTypeCode.value || '').toUpperCase();
  const typeName = (props.dataTypeName || extractedDataTypeName.value || '').toLowerCase();

  // 海表温度（SST - Sea Surface Temperature）
  if (
    typeCode === 'TEMP' ||
    typeCode === 'SST' ||
    typeCode === 'TEMPERATURE' ||
    typeName.includes('温度') ||
    typeName.includes('temperature') ||
    typeName.includes('temp')
  ) {
    return 'sst';
  }

  // 叶绿素浓度
  if (
    typeCode === 'CHL' ||
    typeCode === 'CHLOROPHYLL' ||
    typeName.includes('叶绿素') ||
    typeName.includes('chlorophyll') ||
    typeName.includes('chl')
  ) {
    return 'chlorophyll';
  }

  // 海底地形/深度数据
  if (
    typeCode === 'BATHYMETRY' ||
    typeCode === 'DEPTH' ||
    typeCode === 'BATHY' ||
    typeName.includes('深度') ||
    typeName.includes('地形') ||
    typeName.includes('bathymetry') ||
    typeName.includes('depth') ||
    // 如果数据中包含depth字段且有值，也认为是深度数据
    (props.data && props.data.length > 0 && props.data.some((item) => item.depth != null && item.depth !== 0))
  ) {
    return 'bathymetry';
  }

  // 默认使用2D图表
  return 'chart';
});

/**
 * 处理后的数据，确保数据格式统一（后端返回的数据格式基本不需要转换）
 */
const processedData = computed(() => {
  if (!props.data || props.data.length === 0) {
    return [];
  }

  // 后端返回的数据格式已经符合要求，只需要确保数值类型正确
  return props.data.map((item) => ({
    ...item,
    longitude: item.longitude != null ? Number(item.longitude) : undefined,
    latitude: item.latitude != null ? Number(item.latitude) : undefined,
    dataValue: item.dataValue != null ? Number(item.dataValue) : undefined,
    depth: item.depth != null ? Number(item.depth) : undefined,
  }));
});

/**
 * 计算区域范围（如果未提供，则根据数据自动计算）
 */
const computedRegion = computed(() => {
  if (props.region) {
    return props.region;
  }

  if (!props.data || props.data.length === 0) {
    return {
      minLon: 100,
      maxLon: 130,
      minLat: 0,
      maxLat: 40,
    };
  }

  const longitudes = props.data
    .map((item) => item.longitude)
    .filter((v) => v != null && !isNaN(Number(v)))
    .map((v) => Number(v));
  const latitudes = props.data
    .map((item) => item.latitude)
    .filter((v) => v != null && !isNaN(Number(v)))
    .map((v) => Number(v));

  if (longitudes.length === 0 || latitudes.length === 0) {
    return {
      minLon: 100,
      maxLon: 130,
      minLat: 0,
      maxLat: 40,
    };
  }

  return {
    minLon: Math.min(...longitudes),
    maxLon: Math.max(...longitudes),
    minLat: Math.min(...latitudes),
    maxLat: Math.max(...latitudes),
  };
});

/**
 * 备用图表类型（当使用Chart2DViewer时）
 */
const fallbackChartType = computed(() => {
  const typeCode = (props.dataTypeCode || extractedDataTypeCode.value || '').toUpperCase();
  
  // 根据数据类型选择合适的图表类型
  if (typeCode === 'TEMP' || typeCode === 'SST') {
    return 'HEATMAP';
  }
  if (typeCode === 'CHL' || typeCode === 'CHLOROPHYLL') {
    return 'HEATMAP';
  }
  // 如果有经纬度数据，使用散点图或热力图
  if (props.data && props.data.length > 0) {
    const firstItem = props.data[0];
    if (firstItem.longitude != null && firstItem.latitude != null) {
      return 'SCATTER'; // 如果有经纬度，使用散点图
    }
  }
  return 'LINE'; // 默认折线图
});
</script>

<style scoped lang="less">
.ocean-data-viewer {
  width: 100%;
  height: 100%;
  min-height: 500px;
}
</style>

