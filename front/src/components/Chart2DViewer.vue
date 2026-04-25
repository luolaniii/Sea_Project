<template>
  <div ref="chartRef" class="chart-2d-viewer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
import { parseObservationTimeToMs } from '@/utils/observation-time';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';

interface Props {
  echartsConfig?: string;
  dataQueryConfig?: string;
  chartType?: string;
  data?: any[];
}

const props = defineProps<Props>();

const chartRef = ref<HTMLDivElement>();
let chartInstance: echarts.ECharts | null = null;

type Observation = {
  observationTime?: string;
  dataValue?: number;
  dataTypeCode?: string;
  longitude?: number;
  latitude?: number;
  depth?: number;
};

const parseTimeMs = (t?: string) => {
  const ms = parseObservationTimeToMs(t ?? '');
  return Number.isNaN(ms) ? -1 : ms;
};

const buildDatasetSource = (data: any[]) => {
  if (!data || data.length === 0) return [];

  // 情况1：后端 ObservationDataVO 列表（包含 dataTypeCode） => 聚合成 { time, CODE1, CODE2, ... }
  const first = data[0] as Observation;
  if ((first as any)?.dataTypeCode) {
    const map = new Map<string, any>();
    for (const raw of data as Observation[]) {
      const time = raw.observationTime;
      const code = (raw as any).dataTypeCode;
      if (!time || !code) continue;
      const row = map.get(time) || { time, observationTime: time };
      const v = raw.dataValue != null ? Number(raw.dataValue) : null;
      row[code] = Number.isFinite(v as any) ? v : null;
      // 兼容：部分图表可能想用经纬度/深度字段
      if (raw.longitude != null) row.longitude = raw.longitude;
      if (raw.latitude != null) row.latitude = raw.latitude;
      if (raw.depth != null) row.depth = raw.depth;
      map.set(time, row);
    }
    return Array.from(map.values()).sort((a, b) => parseTimeMs(a.time) - parseTimeMs(b.time));
  }

  // 情况2：已经是“宽表”数据（行里直接有 time/各字段）=> 原样返回
  return data;
};

const tryParseEchartsConfig = (): EChartsOption | null => {
  if (!props.echartsConfig) return null;
  try {
    return parseJsonPreservingLongIds(props.echartsConfig) as EChartsOption;
  } catch (e) {
    console.error('解析ECharts配置失败:', e);
    return null;
  }
};

const buildOption = (): EChartsOption => {
  const data = props.data || [];
  const source = buildDatasetSource(data);

  // 优先使用 SQL / chart_config.echarts_config 的配置（这是你 config.sql 里的“设计稿”）
  const base = tryParseEchartsConfig();
  if (base) {
    return {
      backgroundColor: 'transparent',
      ...base,
      dataset: {
        // 如果 base 自带 dataset，这里用实时数据覆盖 source，避免“配置对但没数据”
        ...(typeof (base as any).dataset === 'object' ? (base as any).dataset : {}),
        source,
      } as any,
    };
  }

  // 没有 echartsConfig 时才走兜底的自动生成
  const option = generateOption();
  return {
    ...option,
    dataset: { source } as any,
  };
};

const initChart = () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);

  const option = buildOption();
  chartInstance.setOption(option, true);
};

const generateOption = (): EChartsOption => {
  const chartType = props.chartType || 'LINE';
  const data = props.data || [];

  switch (chartType) {
    case 'LINE':
      return generateLineOption(data);
    case 'BAR':
      return generateBarOption(data);
    case 'SCATTER':
      return generateScatterOption(data);
    case 'HEATMAP':
      return generateHeatmapOption(data);
    case '3D_SURFACE':
      return generate3DSurfaceOption(data);
    default:
      return generateLineOption(data);
  }
};

const generateLineOption = (data: any[]): EChartsOption => {
  const xData = data.map((item) => item.observationTime || item.x || '');
  const yData = data.map((item) => item.dataValue || item.y || 0);

  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: '#fff',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    series: [
      {
        data: yData,
        type: 'line',
        smooth: true,
        lineStyle: {
          color: '#667eea',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
              { offset: 1, color: 'rgba(102, 126, 234, 0.1)' },
            ],
          },
        },
      },
    ],
  };
};

const generateBarOption = (data: any[]): EChartsOption => {
  const xData = data.map((item) => item.observationTime || item.x || '');
  const yData = data.map((item) => item.dataValue || item.y || 0);

  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: '#fff',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    series: [
      {
        data: yData,
        type: 'bar',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' },
          ]),
        },
      },
    ],
  };
};

const generateScatterOption = (data: any[]): EChartsOption => {
  const scatterData = data.map((item) => [
    item.longitude || item.x || 0,
    item.latitude || item.y || 0,
    item.dataValue || item.value || 0,
  ]);

  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: '#fff',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: '经度',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '纬度',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    series: [
      {
        data: scatterData,
        type: 'scatter',
        symbolSize: (data: number[]) => data[2] * 2,
        itemStyle: {
          color: '#667eea',
        },
      },
    ],
  };
};

const generateHeatmapOption = (data: any[]): EChartsOption => {
  const heatmapData = data.map((item) => [
    item.longitude || item.x || 0,
    item.latitude || item.y || 0,
    item.dataValue || item.value || 0,
  ]);

  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: '#fff',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: '经度',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '纬度',
      axisLine: {
        lineStyle: {
          color: '#fff',
        },
      },
    },
    visualMap: {
      min: 0,
      max: Math.max(...data.map((item) => item.dataValue || item.value || 0)),
      calculable: true,
      inRange: {
        color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffcc', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026'],
      },
      textStyle: {
        color: '#fff',
      },
    },
    series: [
      {
        data: heatmapData,
        type: 'heatmap',
      },
    ],
  };
};

const generate3DSurfaceOption = (data: any[]): EChartsOption => {
  // 3D表面图需要特殊处理，这里简化处理
  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: '#fff',
    },
    xAxis3D: {
      type: 'value',
    },
    yAxis3D: {
      type: 'value',
    },
    zAxis3D: {
      type: 'value',
    },
    grid3D: {
      boxWidth: 200,
      boxDepth: 200,
    },
    series: [
      {
        type: 'surface',
        data: data.map((item) => [
          item.longitude || 0,
          item.latitude || 0,
          item.dataValue || 0,
        ]),
      },
    ],
  };
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

const refresh = () => {
  if (!chartInstance) return;
  const option = buildOption();
  chartInstance.setOption(option, true);
};

watch(
  () => props.echartsConfig,
  () => {
    refresh();
  }
);

watch(
  () => props.data,
  () => {
    refresh();
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
.chart-2d-viewer {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>

