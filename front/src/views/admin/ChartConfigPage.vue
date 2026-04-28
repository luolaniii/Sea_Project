<template>
  <div class="chart-config-page">
    <PageHeader title="图表配置管理">
      <template #extra>
        <button class="btn btn-primary" @click="handleAdd">新增图表</button>
      </template>
    </PageHeader>

    <div class="card">
      <div class="filter-toolbar">
        <div class="filter-item">
          <label>图表分类</label>
          <select v-model="filterChartType" class="filter-select" @change="handleFilterChange">
            <option value="">全部分类</option>
            <option value="LINE">折线图</option>
            <option value="BAR">柱状图</option>
            <option value="SCATTER">散点图</option>
            <option value="HEATMAP">热力图</option>
            <option value="3D_SURFACE">3D 表面图</option>
          </select>
        </div>
        <div class="filter-item">
          <label>站点关键词</label>
          <input
            v-model="filterStationKeyword"
            type="text"
            class="search-input"
            placeholder="站点名或站点ID"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label>站点类型</label>
          <select v-model="filterStationType" class="filter-select" @change="handleFilterChange">
            <option value="">全部站点类型</option>
            <option v-for="t in stationTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div class="filter-item">
          <label>数据类型</label>
          <select v-model="filterDataTypeCode" class="filter-select" @change="handleFilterChange">
            <option value="">全部数据类型</option>
            <option v-for="type in dataTypeList" :key="String(type.id)" :value="type.typeCode">
              {{ type.typeCode }} - {{ type.typeName }}
            </option>
          </select>
        </div>
        <div class="filter-item keyword-item">
          <label>关键词搜索</label>
          <input
            v-model="keyword"
            type="text"
            class="search-input"
            placeholder="输入图表名称关键词"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item coord-item">
          <label>经度范围</label>
          <div class="coord-range">
            <input v-model.number="filterMinLongitude" type="number" class="search-input" placeholder="最小经度" />
            <span>~</span>
            <input v-model.number="filterMaxLongitude" type="number" class="search-input" placeholder="最大经度" />
          </div>
        </div>
        <div class="filter-item coord-item">
          <label>纬度范围</label>
          <div class="coord-range">
            <input v-model.number="filterMinLatitude" type="number" class="search-input" placeholder="最小纬度" />
            <span>~</span>
            <input v-model.number="filterMaxLatitude" type="number" class="search-input" placeholder="最大纬度" />
          </div>
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button class="btn btn-default" @click="handleResetFilters">重置</button>
        </div>
      </div>

      <DataTable
        :columns="columns"
        :data="tableData"
        :actions="actions"
        :loading="loading"
        @action="handleAction"
      >
        <!-- 列表页是否公开开关列 -->
        <template #isPublicDesc="{ row }">
          <label class="switch">
            <input
              type="checkbox"
              :checked="(row as any).isPublicCode === 1 || row.isPublic === 1"
              @change="handleTogglePublic(row)"
            />
            <span class="slider round"></span>
          </label>
        </template>
      </DataTable>
      <Pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadData"
        @page-size-change="handlePageSizeChange"
      />
    </div>

    <Modal v-model:visible="modalVisible" :title="modalTitle" @confirm="handleConfirm">
      <form class="form">
        <div class="form-item">
          <label>图表名称 *</label>
          <input v-model="form.chartName" type="text" placeholder="请输入图表名称" required />
        </div>
        <div class="form-item">
          <label>图表类型 *</label>
          <!-- 与后端 ChartTypeEnum 的 code 对齐，保证编辑时正常回显 -->
          <select v-model="form.chartTypeCode" required>
            <option value="LINE">折线图</option>
            <option value="BAR">柱状图</option>
            <option value="SCATTER">散点图</option>
            <option value="HEATMAP">热力图</option>
            <option value="3D_SURFACE">3D 表面图</option>
          </select>
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="form.description" placeholder="请输入描述" rows="3"></textarea>
        </div>

        <!-- 数据查询配置：用于生成 dataQueryConfig -->
        <div class="form-item">
          <label>数据源（dataSourceId）</label>
          <select v-model="dataQuery.dataSourceId">
            <option :value="undefined">不限数据源</option>
            <option v-for="source in dataSourceList" :key="String(source.id)" :value="source.id != null ? String(source.id) : ''">
              {{ source.sourceName }}
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>数据类型（dataTypeId）</label>
          <select v-model="dataQuery.dataTypeId">
            <option :value="undefined">不限数据类型</option>
            <option v-for="type in dataTypeList" :key="String(type.id)" :value="type.id != null ? String(type.id) : ''">
              {{ type.typeName }} ({{ type.typeCode }}{{ type.unit ? ', ' + type.unit : '' }})
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>数据类型编码列表（typeCodes，逗号/空格分隔，可选）</label>
          <input
            v-model="dataQuery.typeCodesText"
            type="text"
            placeholder="例如：WIND_SPEED, PRESSURE"
          />
        </div>
        <div class="form-item">
          <label>起始时间（startTime，格式：yyyy-MM-dd HH:mm:ss 或 ISO）</label>
          <input
            v-model="dataQuery.startTime"
            type="text"
            placeholder="例如：2026-01-01 00:00:00"
          />
        </div>
        <div class="form-item">
          <label>结束时间（endTime，格式：yyyy-MM-dd HH:mm:ss 或 ISO）</label>
          <input
            v-model="dataQuery.endTime"
            type="text"
            placeholder="例如：2026-01-31 23:59:59"
          />
        </div>
        <div class="form-item">
          <label>经度范围（minLongitude / maxLongitude）</label>
          <div class="range-inputs">
            <input
              v-model.number="dataQuery.minLongitude"
              type="number"
              step="0.0001"
              placeholder="最小经度"
            />
            <span class="range-separator">~</span>
            <input
              v-model.number="dataQuery.maxLongitude"
              type="number"
              step="0.0001"
              placeholder="最大经度"
            />
          </div>
        </div>
        <div class="form-item">
          <label>纬度范围（minLatitude / maxLatitude）</label>
          <div class="range-inputs">
            <input
              v-model.number="dataQuery.minLatitude"
              type="number"
              step="0.0001"
              placeholder="最小纬度"
            />
            <span class="range-separator">~</span>
            <input
              v-model.number="dataQuery.maxLatitude"
              type="number"
              step="0.0001"
              placeholder="最大纬度"
            />
          </div>
        </div>
        <div class="form-item">
          <label>数据条数上限（pageSize）</label>
          <input
            v-model.number="dataQuery.pageSize"
            type="number"
            min="1"
            max="10000"
            placeholder="默认 1000"
          />
        </div>

        <div class="form-item">
          <label>ECharts配置（JSON，可选）</label>
          <textarea
            v-model="form.echartsConfig"
            placeholder='可选的 ECharts JSON 配置；若留空则前端会根据数据自动生成基础配置'
            rows="5"
          ></textarea>
        </div>
      </form>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';
import Modal from '@/components/Modal.vue';
import {
  chartConfigApi,
  type ChartConfig,
  type DataSource,
  type ObservationDataType,
} from '@/utils/api-admin';
import { message } from '@/utils/message';
import { fetchDataSources, fetchObservationDataTypes } from '@/utils/data-meta-cache';
import { buildDataQueryConfigJson, parseDataQueryConfigJson } from '@/utils/data-query-config';

const columns = [
  { key: 'id', title: 'ID', style: { width: '80px' } },
  { key: 'chartName', title: '图表名称' },
  // 使用后端VO中的中文描述字段
  { key: 'chartTypeDesc', title: '图表类型' },
  { key: 'isPublicDesc', title: '是否公开' },
  { key: 'description', title: '描述' },
];

const actions = [
  { key: 'edit', label: '编辑', type: 'primary' as const },
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<ChartConfig[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterChartType = ref('');
const filterStationKeyword = ref('');
const filterStationType = ref('');
const filterDataTypeCode = ref('');
const filterMinLongitude = ref<number | undefined>(undefined);
const filterMaxLongitude = ref<number | undefined>(undefined);
const filterMinLatitude = ref<number | undefined>(undefined);
const filterMaxLatitude = ref<number | undefined>(undefined);
const keyword = ref('');

const modalVisible = ref(false);
const modalTitle = ref('新增图表');
// 扩展表单类型，支持 *_Code 字段用于与后端VO对齐
const form = ref<Partial<ChartConfig & { chartTypeCode?: string; isPublicCode?: number }>>({
  chartName: '',
  chartTypeCode: 'LINE',
  isPublicCode: 0,
  description: '',
  dataQueryConfig: '',
  echartsConfig: '',
});
const editingId = ref<string | number | null>(null);

// 数据源 & 数据类型列表（用于 dataQuery 下拉）
const dataSourceList = ref<DataSource[]>([]);
const dataTypeList = ref<ObservationDataType[]>([]);
const stationTypeLabelMap: Record<string, string> = {
  MET: '标准气象观测站',
  WAVE: '波浪谱观测站',
  OCEAN: '海洋水质观测站',
  DART: 'DART 海啸/水柱高度站',
  CURRENT: '海流剖面观测站',
  WATER_LEVEL: '潮位/水位观测站',
  NOAA: 'NOAA NDBC 站点',
  ERDDAP: 'ERDDAP 科学数据站',
  CUSTOM: '自定义站点',
};

const getStationTypeCode = (ds: any): string => {
  const suffix = String(ds?.fileSuffixes || '').toLowerCase();
  const config = String(ds?.configJson || '').toLowerCase();
  if (suffix.includes('dart') || config.includes('"dart":true') || config.includes('"dart":"y"')) return 'DART';
  if (suffix.includes('adcp') || config.includes('"currents":true') || config.includes('"currents":"y"')) return 'CURRENT';
  if (suffix.includes('tide') || suffix.includes('wlevel')) return 'WATER_LEVEL';
  if (suffix.includes('spec') || suffix.includes('data_spec') || suffix.includes('swden')) return 'WAVE';
  if (suffix.includes('ocean') || config.includes('"waterquality":true') || config.includes('"waterquality":"y"')) return 'OCEAN';
  if (suffix.includes('txt') || suffix.includes('cwind') || suffix.includes('rain') || suffix.includes('srad') || config.includes('"met":true') || config.includes('"met":"y"')) return 'MET';
  return String(ds?.sourceType || '').trim().toUpperCase();
};

const stationTypeOptions = computed(() => {
  const set = new Set<string>();
  dataSourceList.value.forEach((ds) => {
    const t = getStationTypeCode(ds);
    if (t) set.add(t);
  });
  return Array.from(set).map((value) => ({ value, label: stationTypeLabelMap[value] || value }));
});

// dataQuery 配置（会被序列化为 dataQueryConfig JSON）
const dataQuery = ref<{
  dataSourceId?: string | number;
  dataTypeId?: string | number;
  typeCodesText?: string;
  startTime?: string;
  endTime?: string;
  minLongitude?: number;
  maxLongitude?: number;
  minLatitude?: number;
  maxLatitude?: number;
  pageSize?: number;
}>({
  pageSize: 1000,
});

const buildDataQueryConfig = () => {
  return buildDataQueryConfigJson({
    ...dataQuery.value,
    // 允许页面以字符串形式录入 typeCodes
    typeCodes: dataQuery.value.typeCodesText,
  } as any);
};

const parseDataQueryConfig = (configJson?: string) => {
  // 默认值
  dataQuery.value = { pageSize: 1000, typeCodesText: '' };
  if (!configJson) return;
  const parsed = parseDataQueryConfigJson(configJson);
  if (!parsed) return;
  dataQuery.value = {
    dataSourceId: parsed.dataSourceId ?? undefined,
    dataTypeId: parsed.dataTypeId ?? undefined,
    typeCodesText: parsed.typeCodes?.join(',') ?? '',
    startTime: parsed.startTime ?? undefined,
    endTime: parsed.endTime ?? undefined,
    minLongitude: parsed.minLongitude ?? undefined,
    maxLongitude: parsed.maxLongitude ?? undefined,
    minLatitude: parsed.minLatitude ?? undefined,
    maxLatitude: parsed.maxLatitude ?? undefined,
    pageSize: parsed.pageSize ?? 1000,
  };
};

const loadDataSourceList = async () => {
  try {
    const res = await fetchDataSources();
    dataSourceList.value = res;
  } catch (error: any) {
    console.error('加载数据源列表失败:', error);
    message.error(error?.message || '加载数据源列表失败');
  }
};

const loadDataTypeList = async () => {
  try {
    const res = await fetchObservationDataTypes();
    dataTypeList.value = res;
  } catch (error: any) {
    console.error('加载数据类型列表失败:', error);
    message.error(error?.message || '加载数据类型列表失败');
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const chartName = keyword.value.trim();
    const stationKeyword = filterStationKeyword.value.trim();
    const res = await chartConfigApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      chartType: filterChartType.value || undefined,
      chartName: chartName || undefined,
      stationKeyword: stationKeyword || undefined,
      stationType: filterStationType.value || undefined,
      dataTypeCode: filterDataTypeCode.value || undefined,
      minLongitude: filterMinLongitude.value,
      maxLongitude: filterMaxLongitude.value,
      minLatitude: filterMinLatitude.value,
      maxLatitude: filterMaxLatitude.value,
    });
    tableData.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    console.error('加载数据失败:', error);
    message.error(error?.message || '加载数据失败，请稍后重试');
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handlePageSizeChange = () => {
  pageNum.value = 1; // 页大小改变时重置到第一页
  loadData();
};

const handleSearch = () => {
  pageNum.value = 1;
  loadData();
};

const handleFilterChange = () => {
  pageNum.value = 1;
  loadData();
};

const handleResetFilters = () => {
  filterChartType.value = '';
  filterStationKeyword.value = '';
  filterStationType.value = '';
  filterDataTypeCode.value = '';
  filterMinLongitude.value = undefined;
  filterMaxLongitude.value = undefined;
  filterMinLatitude.value = undefined;
  filterMaxLatitude.value = undefined;
  keyword.value = '';
  pageNum.value = 1;
  loadData();
};

// 列表页直接切换是否公开
const handleTogglePublic = async (row: ChartConfig) => {
  const current = (row as any).isPublicCode ?? row.isPublic;
  const old = current;
  const next = current === 1 ? 0 : 1;
  (row as any).isPublicCode = next;
  (row as any).isPublic = next;
  try {
    await chartConfigApi.update({ id: row.id!, isPublic: next } as unknown as ChartConfig);
    message.success(next === 1 ? '已设为公开' : '已设为私有');
  } catch (error: any) {
    console.error('切换公开状态失败:', error);
    (row as any).isPublicCode = old;
    (row as any).isPublic = old;
    message.error(error?.message || '切换公开状态失败，请稍后重试');
  }
};

const handleAdd = () => {
  editingId.value = null;
  modalTitle.value = '新增图表';
  form.value = {
    chartName: '',
    chartTypeCode: 'LINE',
    isPublicCode: 0,
    description: '',
    dataQueryConfig: '',
    echartsConfig: '',
  };
  dataQuery.value = { pageSize: 1000, typeCodesText: '' };
  modalVisible.value = true;
};

const handleAction = async (action: any, row: ChartConfig) => {
  if (action.key === 'edit') {
    editingId.value = row.id!;
    modalTitle.value = '编辑图表';
    // 使用 VO 中的 code 字段进行表单回显
    form.value = {
      ...(row as any),
      chartTypeCode: (row as any).chartTypeCode || (row as any).chartType,
      isPublicCode:
        (row as any).isPublicCode !== undefined ? (row as any).isPublicCode : (row as any).isPublic,
    };
    parseDataQueryConfig(row.dataQueryConfig);
    modalVisible.value = true;
  } else if (action.key === 'delete') {
    if (confirm('确定要删除这条数据吗？')) {
      try {
        await chartConfigApi.delete(row.id!);
        message.success('删除成功');
        loadData();
      } catch (error: any) {
        console.error('删除失败:', error);
        message.error(error?.message || '删除失败，请稍后重试');
      }
    }
  }
};

const handleConfirm = async () => {
  // 表单验证
  if (!form.value.chartName?.trim()) {
    message.warning('请输入图表名称');
    return;
  }
  if (!form.value.chartTypeCode) {
    message.warning('请选择图表类型');
    return;
  }
  
  try {
    const dataQueryConfig = buildDataQueryConfig();
    const f = form.value as any;
    const payload: ChartConfig = {
      ...(form.value as ChartConfig),
      // 显式指定提交给后端的字段，避免把 *_Desc / *_Code 一并提交
      chartName: f.chartName,
      chartType: f.chartTypeCode || f.chartType,
      isPublic: f.isPublicCode ?? f.isPublic ?? 0,
      description: f.description,
      echartsConfig: f.echartsConfig,
      dataQueryConfig,
    };

    if (editingId.value != null && editingId.value !== '') {
      await chartConfigApi.update({ ...payload, id: editingId.value });
      message.success('更新成功');
    } else {
      await chartConfigApi.add(payload);
      message.success('新增成功');
    }
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    console.error('保存失败:', error);
    message.error(error?.message || '保存失败，请稍后重试');
  }
};

onMounted(() => {
  loadData();
  // 预加载数据源 & 数据类型，下次打开弹窗时直接使用缓存
  loadDataSourceList();
  loadDataTypeList();
});
</script>

<style scoped lang="less">
.chart-config-page {
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

.card {
  animation: slideUp 0.5s ease-out 0.2s both;
}

.filter-toolbar {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  align-items: end;
  margin-bottom: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: #334155;
    letter-spacing: 0.2px;
  }
}

.filter-select,
.search-input {
  width: 100%;
  height: 38px;
  border: 1px solid #dbe8f4;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 13px;
  color: #334155;
  background: #ffffff;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: rgba(2, 132, 199, 0.55);
    box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.12);
  }
}

.filter-select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364758b' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 34px;
}

.search-input::placeholder {
  color: #94a3b8;
}

.coord-item .coord-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 6px;

  span {
    color: #64748b;
    font-size: 12px;
    text-align: center;
  }
}

.filter-actions {
  display: inline-flex;
  gap: 8px;
  align-self: end;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.form {
  .form-item {
    margin-bottom: 20px;

    label {
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #334155;
      letter-spacing: 0.2px;
    }

    input,
    textarea,
    select {
      transition: all 0.3s;

      &:focus {
        box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.14);
      }
    }

    select {
      cursor: pointer;
      appearance: none;
      -webkit-appearance: none;
      -moz-appearance: none;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364758b' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 12px center;
      padding-right: 36px;
      color-scheme: light;
      
      &::-ms-expand {
        display: none;
      }
      
      option {
        background: #ffffff !important;
        background-color: #ffffff !important;
        color: #0f172a !important;
        padding: 10px 14px;
      }
      
      option:checked {
        background: #e0f2fe !important;
        background-color: #e0f2fe !important;
        color: #0c4a6e !important;
      }
      
      option:hover,
      option:focus {
        background: #f0f9ff !important;
        background-color: #f0f9ff !important;
        color: #0f172a !important;
      }
    }
  }

  .range-separator {
    color: #64748b;
  }
}

@media (max-width: 960px) {
  .filter-toolbar {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    display: flex;
    gap: 10px;
  }
}
</style>

