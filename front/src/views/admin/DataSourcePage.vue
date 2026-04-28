<template>
  <div class="data-source-page">
    <PageHeader title="数据源管理">
      <template #extra>
        <button class="btn btn-primary" @click="handleAdd">新增数据源</button>
      </template>
    </PageHeader>

    <div class="card">
      <DataTable
        :columns="columns"
        :data="tableData"
        :actions="actions"
        :loading="loading"
        @action="handleAction"
      >
        <!-- 列表页状态开关列 -->
        <template #status="{ row }">
          <label class="switch">
            <input
              type="checkbox"
              :checked="row.status === 1"
              @change="handleToggleStatus(row)"
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
          <label>数据源名称 *</label>
          <input v-model="form.sourceName" type="text" placeholder="请输入数据源名称" required />
        </div>
        <div class="form-item">
          <label>数据源类型 *</label>
          <select v-model="form.sourceType" required>
            <option value="NOAA">NOAA</option>
            <option value="ERDDAP">ERDDAP</option>
            <option value="CUSTOM">自定义</option>
          </select>
        </div>
        <div class="form-item">
          <label>API地址 *</label>
          <input v-model="form.apiUrl" type="text" placeholder="请输入API地址" required />
        </div>
        <div class="form-item">
          <label>API密钥</label>
          <input v-model="form.apiKey" type="text" placeholder="请输入API密钥（可选）" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="form.description" placeholder="请输入描述" rows="3"></textarea>
        </div>

        <!-- NDBC自动采集配置 -->
        <div class="form-section" v-if="form.sourceType === 'NOAA'">
          <h4>NDBC自动采集配置</h4>
          <div class="form-item">
            <label>站点编号</label>
            <div style="display: flex; gap: 8px;">
              <input v-model="form.stationId" type="text" placeholder="例如：TIBC1、WIWF1" style="flex:1" />
              <button type="button" class="btn btn-default" :disabled="fetchingMeta" @click="handleFetchStationMeta">
                {{ fetchingMeta ? '抓取中...' : '自动获取' }}
              </button>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>经度</label>
              <input v-model.number="form.longitude" type="number" step="0.0000001" placeholder="例如：120.123" />
            </div>
            <div class="form-item">
              <label>纬度</label>
              <input v-model.number="form.latitude" type="number" step="0.0000001" placeholder="例如：30.456" />
            </div>
          </div>
          <div class="form-item">
            <label>采集文件后缀</label>
            <input v-model="form.fileSuffixes" type="text" placeholder="逗号分隔，如：txt,ocean,rain" />
            <small>支持: txt(气象), ocean(水质), spec(波谱), cwind(风), rain(降水)</small>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>自动同步</label>
              <label class="switch">
                <input type="checkbox" v-model="autoSyncEnabled" />
                <span class="slider round"></span>
              </label>
            </div>
            <div class="form-item" v-if="autoSyncEnabled">
              <label>同步间隔(分钟)</label>
              <input v-model.number="form.syncIntervalMinutes" type="number" min="5" placeholder="默认30分钟" />
            </div>
          </div>
          <div class="form-item" v-if="form.lastSyncTime">
            <label>最后同步时间</label>
            <span class="info-text">{{ form.lastSyncTime }}</span>
          </div>
        </div>

        <!-- 旧版configJson配置（兼容性保留，仅在编辑时回填） -->
        <div class="form-item">
          <label>站点编号（configJson.stationId）</label>
          <input v-model="config.stationId" type="text" placeholder="例如：32ST0（可选）" />
        </div>
        <div class="form-item">
          <label>经度（configJson.longitude）</label>
          <input v-model="config.longitude" type="text" placeholder="例如：120.123（可选）" />
        </div>
        <div class="form-item">
          <label>纬度（configJson.latitude）</label>
          <input v-model="config.latitude" type="text" placeholder="例如：30.456（可选）" />
        </div>
      </form>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';
import Modal from '@/components/Modal.vue';
import { dataSourceApi, type DataSource } from '@/utils/api-admin';
import { invalidateDataSourceCache } from '@/utils/data-meta-cache';
import { message } from '@/utils/message';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';

const columns = [
  { key: 'id', title: 'ID', style: { width: '80px' } },
  { key: 'sourceName', title: '数据源名称' },
  { key: 'sourceType', title: '类型' },
  { key: 'apiUrl', title: 'API地址' },
  {
    key: 'status',
    title: '状态',
    formatter: (value: number) => (value === 1 ? '启用' : '禁用'),
  },
  { key: 'description', title: '描述' },
];

const actions = [
  { key: 'edit', label: '编辑', type: 'primary' as const },
  { key: 'autogen', label: '一键生成', type: 'primary' as const },
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<DataSource[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const modalVisible = ref(false);
const modalTitle = ref('新增数据源');
const form = ref<Partial<DataSource>>({
  sourceName: '',
  sourceType: 'NOAA',
  apiUrl: '',
  apiKey: '',
  description: '',
  status: 1,
  stationId: '',
  longitude: undefined,
  latitude: undefined,
  fileSuffixes: '',
  autoSync: 0,
  syncIntervalMinutes: 30,
});
const editingId = ref<number | null>(null);
const autoSyncEnabled = ref(false);
const fetchingMeta = ref(false);

const handleFetchStationMeta = async () => {
  const sid = form.value.stationId?.trim();
  if (!sid) {
    message.warning('请先输入站点编号');
    return;
  }
  fetchingMeta.value = true;
  try {
    const meta = await dataSourceApi.ndbcStationMeta(sid);
    if (meta.stationName && !form.value.sourceName) {
      form.value.sourceName = `NOAA NDBC ${sid.toUpperCase()} - ${meta.stationName}`;
    }
    if (meta.latitude != null) form.value.latitude = meta.latitude;
    if (meta.longitude != null) form.value.longitude = meta.longitude;
    if (meta.description) form.value.description = meta.description;
    // 同时写入 configJson
    config.value.stationId = sid.toUpperCase();
    if (meta.latitude != null) config.value.latitude = String(meta.latitude);
    if (meta.longitude != null) config.value.longitude = String(meta.longitude);
    message.success('站点元数据已自动填充');
  } catch (error: any) {
    console.error('抓取站点信息失败:', error);
    message.error(error?.message || '抓取失败');
  } finally {
    fetchingMeta.value = false;
  }
};

// 数据源配置（会序列化到 configJson 字段）
const config = ref<{
  stationId?: string;
  longitude?: string;
  latitude?: string;
}>({});

const buildConfigJson = () => {
  const cfg: any = { ...config.value };
  Object.keys(cfg).forEach((key) => {
    const v = cfg[key];
    if (v === undefined || v === null || v === '') {
      delete cfg[key];
    }
  });
  return Object.keys(cfg).length > 0 ? JSON.stringify(cfg) : '';
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await dataSourceApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
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

// 列表页直接切换启用/禁用状态
const handleToggleStatus = async (row: DataSource) => {
  const oldStatus = row.status;
  const newStatus = row.status === 1 ? 0 : 1;
  row.status = newStatus;
  try {
    await dataSourceApi.update({ id: row.id!, status: newStatus } as DataSource);
    message.success(newStatus === 1 ? '已启用' : '已禁用');
    // 状态变更后，相关缓存也需要失效
    invalidateDataSourceCache();
  } catch (error: any) {
    console.error('切换状态失败:', error);
    row.status = oldStatus;
    message.error(error?.message || '切换状态失败，请稍后重试');
  }
};

const handleAdd = () => {
  editingId.value = null;
  modalTitle.value = '新增数据源';
  form.value = {
    sourceName: '',
    sourceType: 'NOAA',
    apiUrl: '',
    apiKey: '',
    description: '',
    status: 1,
    stationId: '',
    longitude: undefined,
    latitude: undefined,
    fileSuffixes: '',
    autoSync: 0,
    syncIntervalMinutes: 30,
  };
  autoSyncEnabled.value = false;
  config.value = {};
  modalVisible.value = true;
};

const handleAction = async (action: any, row: DataSource) => {
  if (action.key === 'edit') {
    editingId.value = row.id!;
    modalTitle.value = '编辑数据源';
    form.value = { ...row };
    autoSyncEnabled.value = row.autoSync === 1;
     // 回填 configJson
     config.value = {};
     if (row.configJson) {
       try {
         const parsed = parseJsonPreservingLongIds(row.configJson);
         if (parsed && typeof parsed === 'object') {
           config.value = {
             stationId: parsed.stationId,
             longitude: parsed.longitude,
             latitude: parsed.latitude,
           };
         }
       } catch (e) {
         console.error('解析数据源 configJson 失败:', e, row.configJson);
       }
     }
    modalVisible.value = true;
  } else if (action.key === 'autogen') {
    if (!confirm(`将为「${row.sourceName}」一键生成图表和 3D 场景，继续？`)) return;
    try {
      const res = await dataSourceApi.autoGenerate(row.id!);
      message.success(`已生成 ${res.createdCharts} 个图表、${res.createdScenes} 个场景（跳过 ${res.skippedCharts} 个）`);
    } catch (error: any) {
      console.error('自动生成失败:', error);
      message.error(error?.message || '自动生成失败，请稍后重试');
    }
  } else if (action.key === 'delete') {
    if (confirm('确定要删除这条数据吗？')) {
      try {
        await dataSourceApi.delete(row.id!);
        message.success('删除成功');
        // 数据源被删除后，需要让场景管理、图表配置等页面使用的新列表生效
        invalidateDataSourceCache();
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
  if (!form.value.sourceName?.trim()) {
    message.warning('请输入数据源名称');
    return;
  }
  if (!form.value.sourceType) {
    message.warning('请选择数据源类型');
    return;
  }
  if (!form.value.apiUrl?.trim()) {
    message.warning('请输入API地址');
    return;
  }
  
  try {
    const payload: DataSource = {
      ...(form.value as DataSource),
      configJson: buildConfigJson(),
      autoSync: autoSyncEnabled.value ? 1 : 0,
    };

    if (editingId.value) {
      await dataSourceApi.update({ ...payload, id: editingId.value });
      message.success('更新成功');
    } else {
      await dataSourceApi.add(payload);
      message.success('新增成功');
    }
    // 新增或修改数据源后，同样需要刷新缓存
    invalidateDataSourceCache();
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    console.error('保存失败:', error);
    message.error(error?.message || '保存失败，请稍后重试');
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="less">
.data-source-page {
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
  .form-section {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #dbe8f4;

    h4 {
      margin: 0 0 16px 0;
      font-size: 15px;
      color: #334155;
    }
  }

  .form-row {
    display: flex;
    gap: 16px;

    .form-item {
      flex: 1;
    }
  }

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

    small {
      display: block;
      margin-top: 6px;
      font-size: 12px;
      color: #64748b;
    }

    .info-text {
      color: #0284c7;
      font-size: 14px;
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
}
</style>

