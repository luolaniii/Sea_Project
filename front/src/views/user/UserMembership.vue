<template>
  <div class="membership-page">
    <header class="page-hero">
      <div>
        <span class="eyebrow">MEMBERSHIP</span>
        <h1>会员中心</h1>
        <p>使用模拟支付完成充值，系统会立即更新订单、钱包赠币和会员有效期。</p>
      </div>
      <router-link to="/user/orders" class="btn-ghost">我的订单</router-link>
    </header>

    <section class="current-card">
      <span>当前会员</span>
      <strong>{{ membershipActive ? '会员有效' : '未开通或已过期' }}</strong>
      <p>到期时间：{{ membership?.expiresAt ? formatTime(membership.expiresAt) : '-' }}</p>
    </section>

    <section class="plan-grid">
      <article v-for="plan in plans" :key="String(plan.id)" class="plan-card">
        <div class="plan-top">
          <h2>{{ plan.name }}</h2>
          <span>{{ plan.days }} 天</span>
        </div>
        <p>{{ plan.description || '海洋数据平台会员权益' }}</p>
        <div class="price">
          <span>¥</span>{{ Number(plan.priceYuan || 0).toFixed(2) }}
        </div>
        <div class="bonus">赠送 {{ plan.bonusCoins || 0 }} 海洋币</div>
        <button class="btn-primary" :disabled="payingId === String(plan.id)" @click="buy(plan)">
          {{ payingId === String(plan.id) ? '处理中...' : '模拟付款开通' }}
        </button>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { rechargeApi, type MembershipPlan, type UserMembership } from '@/utils/api-expert';
import { message } from '@/utils/message';

const plans = ref<MembershipPlan[]>([]);
const membership = ref<UserMembership | null>(null);
const payingId = ref('');

const membershipActive = computed(() => {
  if (!membership.value?.expiresAt) return false;
  return new Date(membership.value.expiresAt).getTime() > Date.now();
});

const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';

const load = async () => {
  const [p, m] = await Promise.all([rechargeApi.listPlans(), rechargeApi.getMyMembership()]);
  plans.value = p || [];
  membership.value = m || null;
};

const buy = async (plan: MembershipPlan) => {
  const ok = window.confirm(`确认使用模拟支付购买「${plan.name}」？`);
  if (!ok) return;
  payingId.value = String(plan.id);
  try {
    const order = await rechargeApi.createOrder(plan.id);
    await rechargeApi.mockPay(order.id!, 'WECHAT_MOCK');
    message.success('模拟支付成功，会员和赠币已到账');
    await load();
  } finally {
    payingId.value = '';
  }
};

onMounted(load);
</script>

<style scoped lang="less">
@accent: #0284c7;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@hairline: #dbe8f4;

.membership-page { display: flex; flex-direction: column; gap: 18px; }
.page-hero, .current-card, .plan-card {
  border: 1px solid @hairline;
  border-radius: 18px;
  background: rgba(255,255,255,.92);
  box-shadow: 0 16px 44px -30px rgba(15,23,42,.36);
}
.page-hero {
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  background: radial-gradient(circle at 100% 0%, rgba(14,165,233,.18), transparent 42%), #fff;
  h1 { margin: 6px 0 8px; color: @ink-1; font-size: 30px; }
  p { margin: 0; color: @ink-2; }
}
.eyebrow { color: @accent; font-size: 11px; font-weight: 800; letter-spacing: 2px; }
.current-card {
  padding: 22px;
  span { color: @ink-3; font-size: 12px; font-weight: 800; }
  strong { display: block; margin: 8px 0; color: @ink-1; font-size: 24px; }
  p { margin: 0; color: @ink-2; }
}
.plan-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
.plan-card { padding: 22px; display: flex; flex-direction: column; gap: 14px; }
.plan-top { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start;
  h2 { margin: 0; color: @ink-1; font-size: 20px; }
  span { color: @accent; background: #e0f2fe; border: 1px solid #bae6fd; border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 800; }
}
.plan-card p { margin: 0; color: @ink-2; line-height: 1.6; min-height: 44px; }
.price { color: @ink-1; font-size: 36px; font-weight: 900; span { font-size: 18px; margin-right: 3px; color: @ink-3; } }
.bonus { color: #0f766e; background: #ecfeff; border: 1px solid #99f6e4; border-radius: 10px; padding: 9px 12px; font-weight: 800; font-size: 13px; }
.btn-primary, .btn-ghost { border-radius: 10px; padding: 10px 16px; text-decoration: none; font-weight: 800; cursor: pointer; }
.btn-primary { border: none; color: #fff; background: linear-gradient(135deg,#0284c7,#0ea5e9); }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }
.btn-ghost { background: #fff; color: @ink-2; border: 1px solid @hairline; align-self: flex-start; }
@media (max-width:760px){ .page-hero{flex-direction:column;} }
</style>
