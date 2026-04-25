<template>
  <div class="system-config-page">
    <PageHeader title="系统配置管理">
      <template #extra>
        <button class="btn btn-primary" @click="handleAdd">新增配置</button>
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
        <!-- 列表页系统配置开关列 -->
        <template #isSystem="{ row }">
          <label class="switch">
            <input
              type="checkbox"
              :checked="!!row.isSystem"
              @change="handleToggleIsSystem(row)"
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
          <label>配置键 *</label>
          <input v-model="form.configKey" type="text" placeholder="请输入配置键" required />
        </div>
        <div class="form-item">
          <label>配置值 *</label>
          <input v-model="form.configValue" type="text" placeholder="请输入配置值" required />
        </div>
        <div class="form-item">
          <label>配置类型 *</label>
          <select v-model="form.configType" required>
            <option value="STRING">字符串</option>
            <option value="NUMBER">数字</option>
            <option value="BOOLEAN">布尔值</option>
            <option value="JSON">JSON</option>
          </select>
        </div>
        <div class="form-item">
          <label>配置分组</label>
          <input v-model="form.configGroup" type="text" placeholder="请输入配置分组" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="form.description" placeholder="请输入描述" rows="3"></textarea>
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
import { systemConfigApi, type SystemConfig } from '@/utils/api-admin';
import { message } from '@/utils/message';

const columns = [
  { key: 'id', title: 'ID', style: { width: '80px' } },
  { key: 'configKey', title: '配置键' },
  { key: 'configValue', title: '配置值' },
  { key: 'configType', title: '配置类型' },
  { key: 'configGroup', title: '配置分组' },
  {
    key: 'isSystem',
    title: '系统配置',
    formatter: (value: boolean) => (value ? '是' : '否'),
  },
  { key: 'description', title: '描述' },
];

const actions = [
  { key: 'edit', label: '编辑', type: 'primary' as const },
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<SystemConfig[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const modalVisible = ref(false);
const modalTitle = ref('新增配置');
const form = ref<Partial<SystemConfig>>({
  configKey: '',
  configValue: '',
  configType: 'STRING',
  configGroup: '',
  description: '',
  isSystem: false,
});
const editingId = ref<number | null>(null);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await systemConfigApi.page({
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

// 列表页直接切换是否系统配置
const handleToggleIsSystem = async (row: SystemConfig) => {
  const old = !!row.isSystem;
  const next = !old;
  row.isSystem = next;
  try {
    await systemConfigApi.update({ id: row.id!, isSystem: next } as SystemConfig);
    message.success(next ? '已设为系统配置' : '已设为用户配置');
  } catch (error: any) {
    console.error('切换系统配置标记失败:', error);
    row.isSystem = old;
    message.error(error?.message || '切换系统配置标记失败，请稍后重试');
  }
};

const handleAdd = () => {
  editingId.value = null;
  modalTitle.value = '新增配置';
  form.value = {
    configKey: '',
    configValue: '',
    configType: 'STRING',
    configGroup: '',
    description: '',
    isSystem: false,
  };
  modalVisible.value = true;
};

const handleAction = async (action: any, row: SystemConfig) => {
  if (action.key === 'edit') {
    editingId.value = row.id!;
    modalTitle.value = '编辑配置';
    form.value = { ...row };
    modalVisible.value = true;
  } else if (action.key === 'delete') {
    if (confirm('确定要删除这条数据吗？')) {
      try {
        await systemConfigApi.delete(row.id!);
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
  if (!form.value.configKey?.trim()) {
    message.warning('请输入配置键');
    return;
  }
  if (!form.value.configType) {
    message.warning('请选择配置类型');
    return;
  }
  
  try {
    if (editingId.value) {
      await systemConfigApi.update({ ...form.value, id: editingId.value } as SystemConfig);
      message.success('更新成功');
    } else {
      await systemConfigApi.add(form.value as SystemConfig);
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
});
</script>

<style scoped lang="less">
.system-config-page {
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

    label {
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      color: rgba(255, 255, 255, 0.9);
      letter-spacing: 0.2px;
    }

    input,
    textarea,
    select {
      transition: all 0.3s;

      &:focus {
        box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.15);
      }
    }

    select {
      cursor: pointer;
      appearance: none;
      -webkit-appearance: none;
      -moz-appearance: none;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23fff' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 12px center;
      padding-right: 36px;
      color-scheme: dark;
      
      &::-ms-expand {
        display: none;
      }
      
      option {
        background: rgba(15, 20, 45, 0.98) !important;
        background-color: rgba(15, 20, 45, 0.98) !important;
        color: #fff !important;
        padding: 10px 14px;
      }
      
      option:checked {
        background: rgba(102, 126, 234, 0.5) !important;
        background-color: rgba(102, 126, 234, 0.5) !important;
        color: #fff !important;
      }
      
      option:hover,
      option:focus {
        background: rgba(102, 126, 234, 0.4) !important;
        background-color: rgba(102, 126, 234, 0.4) !important;
        color: #fff !important;
      }
    }
  }
}
</style>

