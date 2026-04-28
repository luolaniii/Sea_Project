<template>
  <div class="admin-page">
    <div class="page-title">
      <h1>专家审核</h1>
      <p>审核普通用户的专家申请，通过后用户角色会升级为专家。</p>
    </div>

    <section class="panel">
      <div class="filters">
        <select v-model="status" @change="reload">
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <input v-model.trim="keyword" placeholder="搜索姓名 / 机构 / 方向" @keyup.enter="reload" />
        <button class="btn-primary" @click="reload">搜索</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户ID</th>
              <th>姓名</th>
              <th>机构</th>
              <th>专业方向</th>
              <th>标签</th>
              <th>状态</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="String(item.id)">
              <td>{{ item.id }}</td>
              <td>{{ item.userId }}</td>
              <td>{{ item.realName || '-' }}</td>
              <td>{{ item.organization || '-' }}</td>
              <td>{{ item.profession || '-' }}</td>
              <td>{{ item.expertiseTags || '-' }}</td>
              <td><span class="status" :class="String(item.status || 'PENDING').toLowerCase()">{{ statusText(item.status) }}</span></td>
              <td>{{ formatTime(item.createdTime) }}</td>
              <td class="ops">
                <button v-if="item.status === 'PENDING'" class="op-btn" @click="review(item.id, true)">通过</button>
                <button v-if="item.status === 'PENDING'" class="op-btn danger" @click="review(item.id, false)">驳回</button>
                <span v-else>{{ item.reviewRemark || '-' }}</span>
              </td>
            </tr>
            <tr v-if="!list.length">
              <td colspan="9" class="empty">暂无申请</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @change="load" @page-size-change="reload" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import Pagination from '@/components/Pagination.vue';
import { adminExpertApi, type ExpertApplication } from '@/utils/api-expert';
import { message } from '@/utils/message';

const list = ref<ExpertApplication[]>([]);
const status = ref('');
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';
const statusText = (s?: string) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s || 'PENDING'] || s || '-');

const load = async () => {
  const page = await adminExpertApi.page({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: status.value || undefined,
    keyword: keyword.value || undefined,
  });
  list.value = page.list || [];
  total.value = Number(page.total) || 0;
};

const reload = () => {
  pageNum.value = 1;
  load();
};

const review = async (id: string | number | undefined, approve: boolean) => {
  if (id == null) return;
  const remark = window.prompt(approve ? '通过备注（可留空）' : '驳回原因', approve ? '符合专家审核标准' : '');
  if (remark === null) return;
  await adminExpertApi.review(id, approve, remark);
  message.success(approve ? '已通过申请' : '已驳回申请');
  await load();
};

onMounted(load);
</script>

<style scoped lang="less">
@accent:#0284c7; @ink-1:#0f172a; @ink-2:#334155; @ink-3:#64748b; @hairline:#dbe8f4;
.admin-page{display:flex;flex-direction:column;gap:18px;}
.page-title{h1{margin:0 0 8px;color:@ink-1;font-size:28px;}p{margin:0;color:@ink-2;}}
.panel{border:1px solid @hairline;border-radius:18px;background:rgba(255,255,255,.92);box-shadow:0 16px 44px -30px rgba(15,23,42,.36);}
.filters{padding:18px 20px;border-bottom:1px solid @hairline;display:flex;gap:12px;align-items:center;flex-wrap:wrap;}
select,input{border:1px solid @hairline;border-radius:10px;padding:10px 12px;color:@ink-1;background:#fff;min-width:170px;}
.table-wrap{padding:18px 20px;overflow-x:auto;} table{width:100%;border-collapse:collapse;color:@ink-2;} th,td{padding:12px 10px;border-bottom:1px solid @hairline;text-align:left;font-size:13px;} th{color:@ink-1;background:#f8fbff;font-weight:800;}
.status{display:inline-flex;border-radius:999px;padding:4px 9px;font-weight:800;font-size:12px;background:#fef3c7;color:#b45309;&.approved{background:#dcfce7;color:#15803d;}&.rejected{background:#fee2e2;color:#dc2626;}}
.ops{min-width:120px}.op-btn{border:none;border-radius:8px;background:#0284c7;color:#fff;padding:7px 10px;font-weight:800;cursor:pointer;margin-right:8px;&.danger{background:#dc2626;}}
.empty{text-align:center;color:@ink-3;padding:30px}.btn-primary{border:none;border-radius:10px;padding:10px 16px;background:linear-gradient(135deg,#0284c7,#0ea5e9);color:#fff;font-weight:800;cursor:pointer;}
</style>
