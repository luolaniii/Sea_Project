<template>
  <div class="scene-page">
    <PageHeader title="场景管理">
      <template #extra>
        <button class="btn btn-primary" @click="handleAdd">新增场景</button>
      </template>
    </PageHeader>

    <div class="card">
      <div class="filter-toolbar">
        <div class="filter-item">
          <label>场景名称</label>
          <input
            v-model="filterSceneName"
            type="text"
            class="search-input"
            placeholder="输入场景名称"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label>场景类型</label>
          <select v-model="filterSceneType" class="filter-select" @change="handleFilterChange">
            <option value="">全部场景类型</option>
            <option value="OCEAN_3D">3D 海洋</option>
            <option value="CHART_2D">2D 图表</option>
            <option value="COMPOSITE">复合场景</option>
          </select>
        </div>
        <div class="filter-item">
          <label>公开状态</label>
          <select v-model="filterIsPublic" class="filter-select" @change="handleFilterChange">
            <option value="">全部状态</option>
            <option value="YES">公开</option>
            <option value="NO">私有</option>
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
          <label>海域</label>
          <select v-model="filterOceanRegion" class="filter-select" @change="handleFilterChange">
            <option value="">全部海域</option>
            <option v-for="o in oceanOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
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
          <label>场景名称 *</label>
          <input v-model="form.sceneName" type="text" placeholder="请输入场景名称" required />
        </div>
        <div class="form-item">
          <label>场景类型 *</label>
          <!-- 使用后端约定的 code 值进行绑定，保证编辑时正常回显 -->
          <select v-model="form.sceneTypeCode" required>
            <option value="3D_OCEAN">3D 海洋</option>
            <option value="2D_CHART">2D 图表</option>
            <option value="COMPOSITE">复合场景</option>
          </select>
        </div>
        <div class="form-item">
          <label>缩略图URL</label>
          <input v-model="form.thumbnail" type="text" placeholder="请输入缩略图URL" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="form.description" placeholder="请输入描述" rows="3"></textarea>
        </div>

        <!-- 数据查询配置：用于生成 configJson.dataQuery -->
        <div class="form-item">
          <label>数据源（dataQuery.dataSourceId）</label>
          <select v-model="dataQuery.dataSourceId">
            <option :value="undefined">不限数据源</option>
            <option v-for="source in dataSourceList" :key="String(source.id)" :value="source.id != null ? String(source.id) : ''">
              {{ source.sourceName }}
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>数据类型（dataQuery.dataTypeId）</label>
          <select v-model="dataQuery.dataTypeId">
            <option :value="undefined">不限数据类型</option>
            <option v-for="type in dataTypeList" :key="String(type.id)" :value="type.id != null ? String(type.id) : ''">
              {{ type.typeName }} ({{ type.typeCode }}{{ type.unit ? ', ' + type.unit : '' }})
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>数据类型编码列表（dataQuery.typeCodes，逗号/空格分隔，可选）</label>
          <input
            v-model="dataQuery.typeCodesText"
            type="text"
            placeholder="例如：TEMPERATURE, SALINITY"
          />
        </div>
        <div class="form-item">
          <label>起始时间（dataQuery.startTime，格式：yyyy-MM-dd HH:mm:ss 或 ISO）</label>
          <input
            v-model="dataQuery.startTime"
            type="text"
            placeholder="例如：2026-01-01 00:00:00"
          />
        </div>
        <div class="form-item">
          <label>结束时间（dataQuery.endTime，格式：yyyy-MM-dd HH:mm:ss 或 ISO）</label>
          <input
            v-model="dataQuery.endTime"
            type="text"
            placeholder="例如：2026-01-31 23:59:59"
          />
        </div>
        <div class="form-item">
          <label>经度范围（dataQuery.minLongitude / maxLongitude）</label>
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
          <label>纬度范围（dataQuery.minLatitude / maxLatitude）</label>
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
          <label>数据条数上限（dataQuery.pageSize）</label>
          <input
            v-model.number="dataQuery.pageSize"
            type="number"
            min="1"
            max="10000"
            placeholder="默认 1000"
          />
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
  visualizationSceneApi,
  type VisualizationScene,
  type DataSource,
  type ObservationDataType,
} from '@/utils/api-admin';
import { message } from '@/utils/message';
import { fetchDataSources, fetchObservationDataTypes } from '@/utils/data-meta-cache';
import { buildDataQueryConfigJson, parseDataQueryConfigJson } from '@/utils/data-query-config';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';

const columns = [
  { key: 'id', title: 'ID', style: { width: '80px' } },
  { key: 'sceneName', title: '场景名称' },
  // 使用后端VO中的描述字段，直接展示中文含义
  { key: 'sceneTypeDesc', title: '场景类型' },
  { key: 'isPublicDesc', title: '是否公开' },
  { key: 'viewCount', title: '查看次数' },
  { key: 'description', title: '描述' },
];

const actions = [
  { key: 'edit', label: '编辑', type: 'primary' as const },
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<VisualizationScene[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterSceneName = ref('');
const filterSceneType = ref('');
const filterIsPublic = ref('');
const filterStationKeyword = ref('');
const filterStationType = ref('');
const filterOceanRegion = ref('');
const filterDataTypeCode = ref('');
const filterMinLongitude = ref<number | undefined>(undefined);
const filterMaxLongitude = ref<number | undefined>(undefined);
const filterMinLatitude = ref<number | undefined>(undefined);
const filterMaxLatitude = ref<number | undefined>(undefined);

const modalVisible = ref(false);
const modalTitle = ref('新增场景');
// 扩展表单类型，支持 *_Code 字段用于与后端VO对齐
const form = ref<Partial<VisualizationScene & { sceneTypeCode?: string; isPublicCode?: number }>>({
  sceneName: '',
  sceneTypeCode: '3D_OCEAN',
  isPublicCode: 0,
  description: '',
  configJson: '',
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

const oceanOptions = [
  { value: 'PACIFIC', label: '太平洋' },
  { value: 'ATLANTIC', label: '大西洋' },
  { value: 'INDIAN', label: '印度洋' },
  { value: 'ARCTIC', label: '北冰洋' },
  { value: 'SOUTHERN', label: '南冰洋' },
  { value: 'GULF_OF_MEXICO', label: '墨西哥湾' },
  { value: 'GREAT_LAKES', label: '五大湖' },
];

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

// dataQuery 配置（会被序列化到 configJson.dataQuery）
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

// 将 dataQuery 序列化为场景的 configJson
const buildConfigJson = () => {
  const dqJson = buildDataQueryConfigJson({
    ...dataQuery.value,
    typeCodes: dataQuery.value.typeCodesText,
  } as any);
  const dq = dqJson ? parseJsonPreservingLongIds(dqJson) : null;
  const config: any = {};
  if (dq && typeof dq === 'object' && Object.keys(dq).length > 0) {
    config.dataQuery = dq;
  }
  return Object.keys(config).length > 0 ? JSON.stringify(config) : '';
};

// 从已有的 configJson 中解析 dataQuery 用于回填表单
const parseConfigJsonToDataQuery = (configJson?: string) => {
  // 默认值
  dataQuery.value = { pageSize: 1000, typeCodesText: '' };
  if (!configJson) return;
  try {
    const parsed = parseJsonPreservingLongIds(configJson);
    if (parsed && parsed.dataQuery && typeof parsed.dataQuery === 'object') {
      const dq = parseDataQueryConfigJson(JSON.stringify(parsed.dataQuery));
      if (!dq) return;
      dataQuery.value = {
        dataSourceId: dq.dataSourceId ?? undefined,
        dataTypeId: dq.dataTypeId ?? undefined,
        typeCodesText: dq.typeCodes?.join(',') ?? '',
        startTime: dq.startTime ?? undefined,
        endTime: dq.endTime ?? undefined,
        minLongitude: dq.minLongitude ?? undefined,
        maxLongitude: dq.maxLongitude ?? undefined,
        minLatitude: dq.minLatitude ?? undefined,
        maxLatitude: dq.maxLatitude ?? undefined,
        pageSize: dq.pageSize ?? 1000,
      };
    }
  } catch (e) {
    console.error('解析场景 configJson 失败:', e);
  }
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
    const res = await visualizationSceneApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sceneName: filterSceneName.value.trim() || undefined,
      sceneType: filterSceneType.value || undefined,
      isPublic: filterIsPublic.value || undefined,
      stationKeyword: filterStationKeyword.value.trim() || undefined,
      stationType: filterStationType.value || undefined,
      oceanRegion: filterOceanRegion.value || undefined,
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
  filterSceneName.value = '';
  filterSceneType.value = '';
  filterIsPublic.value = '';
  filterStationKeyword.value = '';
  filterStationType.value = '';
  filterOceanRegion.value = '';
  filterDataTypeCode.value = '';
  filterMinLongitude.value = undefined;
  filterMaxLongitude.value = undefined;
  filterMinLatitude.value = undefined;
  filterMaxLatitude.value = undefined;
  pageNum.value = 1;
  loadData();
};

// 列表页直接切换是否公开
const handleTogglePublic = async (row: VisualizationScene) => {
  const current = (row as any).isPublicCode ?? row.isPublic;
  const old = current;
  const next = current === 1 ? 0 : 1;
  (row as any).isPublicCode = next;
  (row as any).isPublic = next;
  try {
    await visualizationSceneApi.update({ id: row.id!, isPublic: next } as unknown as VisualizationScene);
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
  modalTitle.value = '新增场景';
  form.value = {
    sceneName: '',
    sceneTypeCode: '3D_OCEAN',
    isPublicCode: 0,
    description: '',
    configJson: '',
  };
  dataQuery.value = { pageSize: 1000, typeCodesText: '' };
  modalVisible.value = true;
};

const handleAction = async (action: any, row: VisualizationScene) => {
  if (action.key === 'edit') {
    editingId.value = row.id!;
    modalTitle.value = '编辑场景';
    // 将后端VO中的 code 字段映射到表单，保证下拉框能正确回显
    form.value = {
      ...(row as any),
      sceneTypeCode: (row as any).sceneTypeCode || (row as any).sceneType,
      isPublicCode:
        (row as any).isPublicCode !== undefined ? (row as any).isPublicCode : (row as any).isPublic,
    };
    parseConfigJsonToDataQuery(row.configJson);
    modalVisible.value = true;
  } else if (action.key === 'delete') {
    if (confirm('确定要删除这条数据吗？')) {
      try {
        await visualizationSceneApi.delete(row.id!);
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
  if (!form.value.sceneName?.trim()) {
    message.warning('请输入场景名称');
    return;
  }
  if (!form.value.sceneTypeCode) {
    message.warning('请选择场景类型');
    return;
  }
  
  try {
    // 根据当前 dataQuery 构建场景 configJson
    const configJson = buildConfigJson();
    const f = form.value as any;
    const payload: VisualizationScene = {
      ...(form.value as VisualizationScene),
      // 显式指定要提交给后端的字段，避免把 *_Desc / *_Code 这些VO字段一并提交
      sceneName: f.sceneName,
      sceneType: f.sceneTypeCode || f.sceneType,
      isPublic: f.isPublicCode ?? f.isPublic ?? 0,
      thumbnail: f.thumbnail,
      description: f.description,
      configJson,
    };

    if (editingId.value != null && editingId.value !== '') {
      await visualizationSceneApi.update({ ...payload, id: editingId.value });
      message.success('更新成功');
    } else {
      await visualizationSceneApi.add(payload);
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
.scene-page {
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
  padding: 16px 18px;
  border-bottom: 1px solid #e2e8f0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
  align-items: end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 12px;
    color: #334155;
    font-weight: 600;
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
  color: #334155;
  background: #fff;
  font-size: 13px;

  &:focus {
    outline: none;
    border-color: rgba(2, 132, 199, 0.45);
    box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1);
  }
}

.coord-item .coord-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 6px;
  align-items: center;

  span {
    color: #64748b;
    font-size: 12px;
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
</style>

