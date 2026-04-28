<template>
  <div class="wallet-page">
    <header class="wallet-hero">
      <div>
        <span class="eyebrow">OCEAN WALLET</span>
        <h1>我的钱包</h1>
        <p>查看海洋币余额、专家评估奖励、徽章解锁和近期流水。</p>
      </div>
      <router-link to="/user/membership" class="btn-primary">会员充值</router-link>
    </header>

    <section class="metric-grid">
      <div class="metric-card">
        <span>当前余额</span>
        <strong>{{ wallet?.balanceCoins ?? 0 }}</strong>
        <p>可用于后续平台权益消耗</p>
      </div>
      <div class="metric-card">
        <span>累计获得</span>
        <strong>{{ wallet?.totalEarnedCoins ?? 0 }}</strong>
        <p>评估奖励与充值赠币合计</p>
      </div>
      <div class="metric-card">
        <span>累计消耗</span>
        <strong>{{ wallet?.totalSpentCoins ?? 0 }}</strong>
        <p>退款和消费会记录在流水中</p>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>徽章进度</h2>
        <span>专家评估达到阈值后自动解锁并发放奖励</span>
      </div>
      <div class="badge-grid">
        <div v-for="badge in badges" :key="String(badge.id)" class="badge-card" :class="{ unlocked: isUnlocked(badge.id) }">
          <div class="badge-mark">{{ badge.code?.slice(0, 1) || 'B' }}</div>
          <div>
            <strong>{{ badge.name }}</strong>
            <p>{{ badge.description || `完成 ${badge.thresholdCount} 次评估` }}</p>
            <span>{{ badge.thresholdCount }} 次评估 · 奖励 {{ badge.rewardCoins }} 币</span>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>近期流水</h2>
        <button class="btn-ghost" @click="load">刷新</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>类型</th>
              <th>金额</th>
              <th>余额</th>
              <th>说明</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tx in transactions" :key="String(tx.id)">
              <td>{{ txType(tx.type) }}</td>
              <td :class="Number(tx.amount) >= 0 ? 'pos' : 'neg'">{{ Number(tx.amount) >= 0 ? '+' : '' }}{{ tx.amount }}</td>
              <td>{{ tx.balanceAfter }}</td>
              <td>{{ tx.remark || '-' }}</td>
              <td>{{ formatTime(tx.createdTime) }}</td>
            </tr>
            <tr v-if="!transactions.length">
              <td colspan="5" class="empty">暂无流水</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { walletApi, type Badge, type UserBadge, type UserWallet, type WalletTransaction } from '@/utils/api-expert';

const wallet = ref<UserWallet | null>(null);
const transactions = ref<WalletTransaction[]>([]);
const catalog = ref<Badge[]>([]);
const userBadges = ref<UserBadge[]>([]);

const badges = computed(() => [...catalog.value].sort((a, b) => Number(a.thresholdCount) - Number(b.thresholdCount)));
const unlockedIds = computed(() => new Set(userBadges.value.map((b) => String(b.badgeId))));

const isUnlocked = (id?: string | number) => id != null && unlockedIds.value.has(String(id));

const txType = (type: string) => {
  const map: Record<string, string> = {
    EARN_REVIEW: '评估奖励',
    EARN_BADGE: '徽章奖励',
    RECHARGE_BONUS: '充值赠币',
    SPEND: '消费',
    REFUND: '退款',
  };
  return map[type] || type || '-';
};

const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';

const load = async () => {
  const [w, tx, ub, bc] = await Promise.all([
    walletApi.getMyWallet(),
    walletApi.getTransactions(80),
    walletApi.getMyBadges(),
    walletApi.getBadgeCatalog(),
  ]);
  wallet.value = w;
  transactions.value = tx || [];
  userBadges.value = ub || [];
  catalog.value = bc || [];
};

onMounted(load);
</script>

<style scoped lang="less">
@accent: #0284c7;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@hairline: #dbe8f4;
@paper: #ffffff;

.wallet-page { display: flex; flex-direction: column; gap: 18px; }
.wallet-hero,
.panel,
.metric-card {
  border: 1px solid @hairline;
  border-radius: 18px;
  background: rgba(255,255,255,0.92);
  box-shadow: 0 16px 44px -30px rgba(15,23,42,0.36);
}
.wallet-hero {
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: radial-gradient(circle at 100% 0%, rgba(14,165,233,0.18), transparent 42%), #fff;
  h1 { margin: 6px 0 8px; color: @ink-1; font-size: 30px; }
  p { margin: 0; color: @ink-2; }
}
.eyebrow { color: @accent; font-size: 11px; font-weight: 800; letter-spacing: 2px; }
.metric-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.metric-card {
  padding: 22px;
  span { color: @ink-3; font-size: 12px; font-weight: 800; }
  strong { display: block; margin: 8px 0; color: @ink-1; font-size: 32px; }
  p { margin: 0; color: @ink-3; font-size: 13px; }
}
.panel-head {
  padding: 18px 22px;
  border-bottom: 1px solid @hairline;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  h2 { margin: 0; font-size: 18px; color: @ink-1; }
  span { color: @ink-3; font-size: 13px; }
}
.badge-grid { padding: 20px; display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 14px; }
.badge-card {
  display: flex; gap: 14px; padding: 16px; border: 1px solid @hairline; border-radius: 14px; background: #f8fbff; opacity: .68;
  &.unlocked { opacity: 1; border-color: #7dd3fc; background: #f0f9ff; }
  strong { color: @ink-1; }
  p { margin: 5px 0; color: @ink-2; font-size: 13px; line-height: 1.5; }
  span { color: @ink-3; font-size: 12px; }
}
.badge-mark { width: 42px; height: 42px; border-radius: 13px; background: linear-gradient(135deg,#0284c7,#06b6d4); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 900; }
.table-wrap { padding: 18px 20px; overflow-x: auto; }
table { width: 100%; border-collapse: collapse; color: @ink-2; }
th, td { padding: 12px 10px; border-bottom: 1px solid @hairline; text-align: left; font-size: 13px; }
th { color: @ink-1; background: #f8fbff; font-weight: 800; }
.pos { color: #16a34a; font-weight: 800; }
.neg { color: #dc2626; font-weight: 800; }
.empty { text-align: center; color: @ink-3; padding: 30px; }
.btn-primary, .btn-ghost { border-radius: 10px; padding: 10px 16px; text-decoration: none; font-weight: 800; cursor: pointer; }
.btn-primary { background: linear-gradient(135deg,#0284c7,#0ea5e9); color: #fff; border: none; }
.btn-ghost { background: #fff; color: @ink-2; border: 1px solid @hairline; }
@media (max-width: 760px) {
  .metric-grid { grid-template-columns: 1fr; }
  .wallet-hero { flex-direction: column; align-items: flex-start; }
}
</style>
