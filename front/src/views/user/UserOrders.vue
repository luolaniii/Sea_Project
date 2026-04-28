<template>
  <div class="orders-page">
    <header class="page-hero">
      <div>
        <span class="eyebrow">MY ORDERS</span>
        <h1>我的订单</h1>
        <p>查看模拟充值订单状态，待支付订单可继续模拟付款或取消。</p>
      </div>
      <router-link to="/user/membership" class="btn-primary">去充值</router-link>
    </header>

    <section class="panel">
      <div class="filters">
        <select v-model="status" @change="reload">
          <option value="">全部状态</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="CANCELLED">已取消</option>
          <option value="REFUNDED">已退款</option>
        </select>
        <button class="btn-ghost" @click="reload">刷新</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>套餐ID</th>
              <th>金额</th>
              <th>状态</th>
              <th>支付方式</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="String(order.id)">
              <td class="mono">{{ order.orderNo }}</td>
              <td>{{ order.planId }}</td>
              <td>¥{{ Number(order.amountYuan || 0).toFixed(2) }}</td>
              <td><span class="status" :class="String(order.status).toLowerCase()">{{ statusText(order.status) }}</span></td>
              <td>{{ order.mockPayMethod || '-' }}</td>
              <td>{{ formatTime(order.createdTime) }}</td>
              <td>
                <button v-if="order.status === 'PENDING'" class="link-btn" @click="pay(order.id)">模拟付款</button>
                <button v-if="order.status === 'PENDING'" class="link-btn danger" @click="cancel(order.id)">取消</button>
                <span v-else>-</span>
              </td>
            </tr>
            <tr v-if="!orders.length">
              <td colspan="7" class="empty">暂无订单</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="load"
        @page-size-change="reload"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import Pagination from '@/components/Pagination.vue';
import { rechargeApi, type RechargeOrder } from '@/utils/api-expert';
import { message } from '@/utils/message';

const orders = ref<RechargeOrder[]>([]);
const status = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';
const statusText = (s: string) => ({ PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款' }[s] || s);

const load = async () => {
  const page = await rechargeApi.getMyOrders({ pageNum: pageNum.value, pageSize: pageSize.value, status: status.value || undefined });
  orders.value = page.list || [];
  total.value = Number(page.total) || 0;
};

const reload = () => {
  pageNum.value = 1;
  load();
};

const pay = async (id?: string | number) => {
  if (id == null || !window.confirm('确认执行模拟付款？')) return;
  await rechargeApi.mockPay(id, 'WECHAT_MOCK');
  message.success('模拟付款成功');
  await load();
};

const cancel = async (id?: string | number) => {
  if (id == null || !window.confirm('确认取消该订单？')) return;
  await rechargeApi.cancelOrder(id);
  message.success('订单已取消');
  await load();
};

onMounted(load);
</script>

<style scoped lang="less">
@accent:#0284c7; @ink-1:#0f172a; @ink-2:#334155; @ink-3:#64748b; @hairline:#dbe8f4;
.orders-page{display:flex;flex-direction:column;gap:18px;}
.page-hero,.panel{border:1px solid @hairline;border-radius:18px;background:rgba(255,255,255,.92);box-shadow:0 16px 44px -30px rgba(15,23,42,.36);}
.page-hero{padding:28px 32px;display:flex;justify-content:space-between;gap:20px;background:radial-gradient(circle at 100% 0%,rgba(14,165,233,.18),transparent 42%),#fff;
  h1{margin:6px 0 8px;color:@ink-1;font-size:30px;} p{margin:0;color:@ink-2;}
}
.eyebrow{color:@accent;font-size:11px;font-weight:800;letter-spacing:2px;}
.filters{padding:18px 20px;border-bottom:1px solid @hairline;display:flex;gap:12px;align-items:center;}
select{border:1px solid @hairline;border-radius:10px;padding:10px 34px 10px 12px;color:@ink-1;background:#fff;}
.table-wrap{padding:18px 20px;overflow-x:auto;}
table{width:100%;border-collapse:collapse;color:@ink-2;} th,td{padding:12px 10px;border-bottom:1px solid @hairline;text-align:left;font-size:13px;} th{color:@ink-1;background:#f8fbff;font-weight:800;}
.mono{font-family:'JetBrains Mono','Consolas',monospace;}
.status{display:inline-flex;border-radius:999px;padding:4px 9px;font-weight:800;font-size:12px;background:#f1f5f9;color:@ink-2;}
.status.paid{background:#dcfce7;color:#15803d}.status.pending{background:#fef3c7;color:#b45309}.status.cancelled{background:#f1f5f9;color:#64748b}.status.refunded{background:#fee2e2;color:#dc2626}
.link-btn{border:none;background:none;color:@accent;font-weight:800;cursor:pointer;margin-right:8px;&.danger{color:#dc2626;}}
.empty{text-align:center;color:@ink-3;padding:30px;}
.btn-primary,.btn-ghost{border-radius:10px;padding:10px 16px;text-decoration:none;font-weight:800;cursor:pointer;}
.btn-primary{background:linear-gradient(135deg,#0284c7,#0ea5e9);color:#fff;border:none}.btn-ghost{background:#fff;color:@ink-2;border:1px solid @hairline;}
@media(max-width:760px){.page-hero{flex-direction:column;}.filters{flex-wrap:wrap;}}
</style>
