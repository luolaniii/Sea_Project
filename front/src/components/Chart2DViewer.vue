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

const LIGHT_TEXT = '#334155';
const LIGHT_AXIS = '#94a3b8';

const normalizeChartType = (raw?: string) => {
  const source = String(raw || '').trim();
  const upper = source.toUpperCase();
  const map: Record<string, string> = {
    LINE: 'LINE',
    折线图: 'LINE',
    BAR: 'BAR',
    柱状图: 'BAR',
    SCATTER: 'SCATTER',
    散点图: 'SCATTER',
    HEATMAP: 'HEATMAP',
    热力图: 'HEATMAP',
    '3D_SURFACE': '3D_SURFACE',
    '3D表面图': '3D_SURFACE',
    '3D 表面图': '3D_SURFACE',
  };
  return map[source] || map[upper] || 'LINE';
};

const sanitizeEchartsConfig = (config: EChartsOption): EChartsOption => {
  const safe = { ...(config as any) };
  if (Array.isArray((safe as any).series)) {
    (safe as any).series = (safe as any).series.map((item: any) => {
      if (!item || typeof item !== 'object') return item;
      if (item.type === 'surface') {
        // 前端未引入 echarts-gl，surface 类型会导致渲染失败，这里自动降级为折线图
        return {
          ...item,
          type: 'line',
          smooth: true,
        };
      }
      return item;
    });
  }
  return safe;
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
  const rawData = props.data || [];
  const source = buildDatasetSource(rawData);

  // 有实时数据时，优先走统一兜底渲染，避免被历史 echartsConfig（暗色/不兼容配置）渲染成空白
  if (rawData.length > 0 || source.length > 0) {
    const option = generateOption();
    return {
      ...option,
      dataset: { source } as any,
    };
  }

  // 没有实时数据时才回退到配置稿，尽量给出结构化占位
  const base = tryParseEchartsConfig();
  if (base) {
    const safeBase = sanitizeEchartsConfig(base);
    return {
      backgroundColor: 'transparent',
      ...safeBase,
    };
  }

  return generateLineOption([]);
};

const initChart = () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);

  try {
    const option = buildOption();
    chartInstance.setOption(option, true);
  } catch (e) {
    console.error('设置图表配置失败，使用兜底折线图:', e);
    chartInstance.setOption(generateLineOption(props.data || []), true);
  }
};

const generateOption = (): EChartsOption => {
  const chartType = normalizeChartType(props.chartType);
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
      return generateHeatmapOption(data);
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
      color: LIGHT_TEXT,
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
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
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
      color: LIGHT_TEXT,
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
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
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
      color: LIGHT_TEXT,
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
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '纬度',
      axisLine: {
        lineStyle: {
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
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
  const maxValue = data.length > 0
    ? Math.max(...data.map((item) => Number(item.dataValue || item.value || 0)))
    : 100;

  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: LIGHT_TEXT,
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
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '纬度',
      axisLine: {
        lineStyle: {
          color: LIGHT_AXIS,
        },
      },
      axisLabel: {
        color: LIGHT_TEXT,
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
        },
      },
    },
    visualMap: {
      min: 0,
      max: Number.isFinite(maxValue) && maxValue > 0 ? maxValue : 100,
      calculable: true,
      inRange: {
        color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffcc', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026'],
      },
      textStyle: {
        color: LIGHT_TEXT,
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
  try {
    const option = buildOption();
    chartInstance.setOption(option, true);
  } catch (e) {
    console.error('刷新图表失败，使用兜底折线图:', e);
    chartInstance.setOption(generateLineOption(props.data || []), true);
  }
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

watch(
  () => props.chartType,
  () => {
    refresh();
  }
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

