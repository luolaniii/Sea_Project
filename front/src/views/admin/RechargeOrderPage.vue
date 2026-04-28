<template>
  <div class="admin-page">
    <div class="page-title">
      <h1>充值订单管理</h1>
      <p>查看模拟充值订单流水，已支付订单可执行模拟退款。</p>
    </div>

    <section class="panel">
      <div class="filters">
        <select v-model="status" @change="reload">
          <option value="">全部状态</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="CANCELLED">已取消</option>
          <option value="REFUNDED">已退款</option>
        </select>
        <input v-model.trim="orderNo" placeholder="搜索订单号" @keyup.enter="reload" />
        <button class="btn-primary" @click="reload">搜索</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户ID</th>
              <th>套餐ID</th>
              <th>金额</th>
              <th>状态</th>
              <th>支付方式</th>
              <th>支付时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="String(item.id)">
              <td class="mono">{{ item.orderNo }}</td>
              <td>{{ item.userId }}</td>
              <td>{{ item.planId }}</td>
              <td>¥{{ Number(item.amountYuan || 0).toFixed(2) }}</td>
              <td><span class="status" :class="String(item.status).toLowerCase()">{{ statusText(item.status) }}</span></td>
              <td>{{ item.mockPayMethod || '-' }}</td>
              <td>{{ formatTime(item.paidAt || item.createdTime) }}</td>
              <td>
                <button v-if="item.status === 'PAID'" class="op-btn danger" @click="refund(item.id)">退款</button>
                <span v-else>{{ item.refundRemark || '-' }}</span>
              </td>
            </tr>
            <tr v-if="!list.length">
              <td colspan="8" class="empty">暂无订单</td>
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
import { adminRechargeApi, type RechargeOrder } from '@/utils/api-expert';
import { message } from '@/utils/message';

const list = ref<RechargeOrder[]>([]);
const status = ref('');
const orderNo = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';
const statusText = (s: string) => ({ PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款' }[s] || s);

const load = async () => {
  const page = await adminRechargeApi.page({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: status.value || undefined,
    orderNo: orderNo.value || undefined,
  });
  list.value = page.list || [];
  total.value = Number(page.total) || 0;
};

const reload = () => {
  pageNum.value = 1;
  load();
};

const refund = async (id?: string | number) => {
  if (id == null) return;
  const remark = window.prompt('退款备注', '管理员模拟退款');
  if (remark === null) return;
  await adminRechargeApi.refund(id, remark);
  message.success('退款完成');
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
.mono{font-family:'JetBrains Mono','Consolas',monospace;}
.status{display:inline-flex;border-radius:999px;padding:4px 9px;font-weight:800;font-size:12px;background:#f1f5f9;color:@ink-2;&.paid{background:#dcfce7;color:#15803d;}&.pending{background:#fef3c7;color:#b45309;}&.refunded{background:#fee2e2;color:#dc2626;}}
.op-btn{border:none;border-radius:8px;background:#0284c7;color:#fff;padding:7px 10px;font-weight:800;cursor:pointer;&.danger{background:#dc2626;}}
.empty{text-align:center;color:@ink-3;padding:30px}.btn-primary{border:none;border-radius:10px;padding:10px 16px;background:linear-gradient(135deg,#0284c7,#0ea5e9);color:#fff;font-weight:800;cursor:pointer;}
</style>
