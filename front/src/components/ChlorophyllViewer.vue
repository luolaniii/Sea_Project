<template>
  <div ref="chartRef" class="chlorophyll-viewer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
import 'echarts-gl';

interface Props {
  data?: any[];
  region?: {
    minLon: number;
    maxLon: number;
    minLat: number;
    maxLat: number;
  };
  config?: any;
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  region: () => ({
    minLon: 100,
    maxLon: 130,
    minLat: 0,
    maxLat: 40,
  }),
});

const chartRef = ref<HTMLDivElement>();
let chartInstance: echarts.ECharts | null = null;

const initChart = () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);

  const option = generateOption();
  chartInstance.setOption(option);
};

const generateOption = (): EChartsOption => {
  const data = props.data || [];
  const region = props.region || {
    minLon: 100,
    maxLon: 130,
    minLat: 0,
    maxLat: 40,
  };

  // 处理数据：将经纬度和叶绿素浓度值转换为热力图格式
  const heatmapData: number[][] = data.map((item) => [
    item.longitude || 0,
    item.latitude || 0,
    item.dataValue || item.chlorophyll || 0,
  ]);

  // 计算叶绿素浓度范围
  const concentrations = data.map((item) => item.dataValue || item.chlorophyll || 0);
  const minConc = Math.min(...concentrations);
  const maxConc = Math.max(...concentrations);

  return {
    backgroundColor: 'transparent',
    title: {
      text: '叶绿素浓度分布图',
      left: 'center',
      top: 20,
      textStyle: {
        color: '#fff',
        fontSize: 18,
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.data && Array.isArray(params.data)) {
          return `经度: ${params.data[0].toFixed(2)}°<br/>纬度: ${params.data[1].toFixed(2)}°<br/>叶绿素浓度: ${params.data[2].toFixed(4)} mg/m³`;
        }
        return '';
      },
    },
    visualMap: {
      min: minConc,
      max: maxConc,
      calculable: true,
      realtime: false,
      inRange: {
        // 从深蓝（低浓度）到绿色（高浓度）
        color: [
          '#000080',
          '#0000ff',
          '#0080ff',
          '#00ffff',
          '#80ff80',
          '#ffff00',
          '#ff8000',
          '#ff0000',
        ],
      },
      textStyle: {
        color: '#fff',
      },
      left: 'left',
      top: 'bottom',
    },
    geo: {
      map: 'world',
      roam: true,
      itemStyle: {
        areaColor: '#142547',
        borderColor: '#0692a4',
      },
      emphasis: {
        itemStyle: {
          areaColor: '#09132e',
        },
      },
    },
    series: [
      {
        name: '叶绿素浓度',
        type: 'heatmap',
        coordinateSystem: 'geo',
        data: heatmapData,
        pointSize: 20,
        blurSize: 15,
      },
    ],
  };
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

watch(
  () => props.data,
  () => {
    if (chartInstance) {
      const option = generateOption();
      chartInstance.setOption(option, true);
    }
  },
  { deep: true }
);

watch(
  () => props.region,
  () => {
    if (chartInstance) {
      const option = generateOption();
      chartInstance.setOption(option, true);
    }
  },
  { deep: true }
);

onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped lang="less">
.chlorophyll-viewer {
  width: 100%;
  height: 100%;
  min-height: 500px;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1f3a 100%);
}
</style>

