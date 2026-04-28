<template>
  <div class="observation-data-page">
    <PageHeader title="观测数据管理">
      <template #extra>
        <button class="btn btn-primary" @click="handleAdd">新增数据</button>
      </template>
    </PageHeader>

    <div class="card">
      <DataTable
        :columns="columns"
        :data="tableData"
        :actions="actions"
        :loading="loading"
        @action="handleAction"
      />
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
          <label>数据源 *</label>
          <select v-model="form.dataSourceId" required>
            <option value="">请选择数据源</option>
            <option v-for="source in dataSourceList" :key="String(source.id)" :value="source.id != null ? String(source.id) : ''">
              {{ source.sourceName }}
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>数据类型 *</label>
          <select v-model="form.dataTypeId" required>
            <option value="">请选择数据类型</option>
            <option v-for="type in dataTypeList" :key="String(type.id)" :value="type.id != null ? String(type.id) : ''">
              {{ type.typeName }} ({{ type.typeCode }}{{ type.unit ? `, ${type.unit}` : '' }})
            </option>
          </select>
        </div>
        <div class="form-item">
          <label>观测时间 *</label>
          <input v-model="form.observationTime" type="datetime-local" required />
        </div>
        <div class="form-item">
          <label>数据值 *</label>
          <input v-model.number="form.dataValue" type="number" step="0.01" placeholder="请输入数据值" required />
        </div>
        <div class="form-item">
          <label>经度 *</label>
          <input v-model.number="form.longitude" type="number" step="0.0001" placeholder="请输入经度" required />
        </div>
        <div class="form-item">
          <label>纬度 *</label>
          <input v-model.number="form.latitude" type="number" step="0.0001" placeholder="请输入纬度" required />
        </div>
        <div class="form-item">
          <label>深度（米）</label>
          <input v-model.number="form.depth" type="number" step="0.01" placeholder="请输入深度" />
        </div>
        <div class="form-item">
          <label>质量标志</label>
          <select v-model="form.qualityFlag">
            <option value="GOOD">GOOD</option>
            <option value="QUESTIONABLE">QUESTIONABLE</option>
            <option value="BAD">BAD</option>
          </select>
        </div>
        <div class="form-item">
          <label>备注</label>
          <textarea v-model="form.remark" placeholder="请输入备注" rows="3"></textarea>
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
import { 
  observationDataApi, 
  dataSourceApi, 
  observationDataTypeApi,
  type ObservationData,
  type DataSource,
  type ObservationDataType
} from '@/utils/api-admin';
import { message } from '@/utils/message';

const columns = [
  { key: 'id', title: 'ID', style: { width: '80px' } },
  { 
    key: 'dataSourceId', 
    title: '数据源',
    formatter: (value: any, row: any) => {
      return row.dataSourceName || value || '-';
    }
  },
  { 
    key: 'dataTypeId', 
    title: '数据类型',
    formatter: (value: any, row: any) => {
      return row.dataTypeName || value || '-';
    }
  },
  { key: 'observationTime', title: '观测时间' },
  { 
    key: 'dataValue', 
    title: '数据值',
    formatter: (value: any, row: any) => {
      const unit = row.dataTypeUnit ? ` ${row.dataTypeUnit}` : '';
      return value !== null && value !== undefined ? `${value}${unit}` : '-';
    }
  },
  { key: 'longitude', title: '经度' },
  { key: 'latitude', title: '纬度' },
  { key: 'depth', title: '深度' },
  { 
    key: 'qualityFlag', 
    title: '质量标志',
    formatter: (value: any, row: any) => {
      return row.qualityFlagDesc || value || '-';
    }
  },
];

const actions = [
  { key: 'edit', label: '编辑', type: 'primary' as const },
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<ObservationData[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const modalVisible = ref(false);
const modalTitle = ref('新增观测数据');
const form = ref<Partial<ObservationData>>({});
const editingId = ref<string | number | null>(null);

// 数据源和数据类型列表
const dataSourceList = ref<DataSource[]>([]);
const dataTypeList = ref<ObservationDataType[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await observationDataApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    tableData.value = res.list || [];
    total.value = res.total || 0;
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

const handleAdd = () => {
  editingId.value = null;
  modalTitle.value = '新增观测数据';
  form.value = {};
  modalVisible.value = true;
};

const handleAction = async (action: any, row: ObservationData) => {
  if (action.key === 'edit') {
    editingId.value = row.id!;
    modalTitle.value = '编辑观测数据';
    form.value = { ...row };
    // 转换时间格式
    if (form.value.observationTime) {
      const date = new Date(form.value.observationTime);
      form.value.observationTime = date.toISOString().slice(0, 16);
    }
    modalVisible.value = true;
  } else if (action.key === 'delete') {
    if (confirm('确定要删除这条数据吗？')) {
      try {
        await observationDataApi.delete(row.id!);
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
  if (!form.value.dataSourceId) {
    message.warning('请选择数据源');
    return;
  }
  if (!form.value.dataTypeId) {
    message.warning('请选择数据类型');
    return;
  }
  if (!form.value.observationTime) {
    message.warning('请选择观测时间');
    return;
  }
  
  try {
    if (editingId.value != null && editingId.value !== '') {
      await observationDataApi.update({ ...form.value, id: editingId.value } as ObservationData);
      message.success('更新成功');
    } else {
      await observationDataApi.add(form.value as ObservationData);
      message.success('新增成功');
    }
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    console.error('保存失败:', error);
    message.error(error?.message || '保存失败，请稍后重试');
  }
};

// 加载数据源和数据类型列表
const loadDataSourceList = async () => {
  try {
    const res = await dataSourceApi.list();
    dataSourceList.value = res || [];
  } catch (error: any) {
    console.error('加载数据源列表失败:', error);
    message.error(error?.message || '加载数据源列表失败');
  }
};

const loadDataTypeList = async () => {
  try {
    const res = await observationDataTypeApi.list();
    dataTypeList.value = res || [];
  } catch (error: any) {
    console.error('加载数据类型列表失败:', error);
    message.error(error?.message || '加载数据类型列表失败');
  }
};

onMounted(() => {
  loadData();
  loadDataSourceList();
  loadDataTypeList();
});
</script>

<style scoped lang="less">
.observation-data-page {
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
  .form-item {
    margin-bottom: 20px;
    position: relative;
    z-index: 1;

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
      position: relative;
      z-index: 10;
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

