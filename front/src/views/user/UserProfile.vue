<template>
  <div class="user-profile">
    <div class="profile-page-header">
      <h1 class="profile-page-title">我的账户</h1>
    </div>

    <div class="profile-grid">
      <!-- 主面板：账户卡 -->
      <section class="card profile-card">
        <div class="profile-banner"></div>
        <div class="profile-head">
          <div
            class="profile-avatar"
            :class="{ 'has-image': hasAvatar, uploading: avatarUploading }"
            role="button"
            tabindex="0"
            :title="hasAvatar ? '点击更换头像' : '点击上传头像'"
            @click="triggerAvatarInput"
            @keydown.enter.space.prevent="triggerAvatarInput"
          >
            <img
              v-if="hasAvatar"
              :src="avatarSrc"
              alt="avatar"
              @error="onAvatarError"
            />
            <span v-else>{{ avatarChar }}</span>
            <div class="avatar-mask">
              <Icon name="upload" :size="16" color="#fff" />
              <span>{{ avatarUploading ? '上传中…' : '更换头像' }}</span>
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/png,image/jpeg,image/jpg,image/gif,image/webp"
            class="avatar-input"
            @change="handleAvatarChange"
          />
          <div class="profile-meta">
            <h2 class="profile-name">{{ user?.username || '未登录' }}</h2>
            <div class="profile-tags">
              <span class="tag tag-role">
                <Icon name="user" :size="11" />
                {{ roleLabel }}
              </span>
              <span class="tag tag-id">ID · {{ String(user?.id ?? '—') }}</span>
              <span class="tag tag-status">
                <span class="dot"></span>
                在线
              </span>
            </div>
          </div>
          <button class="btn-logout" @click="handleLogout">
            <Icon name="logout" :size="14" />
            <span>退出登录</span>
          </button>
        </div>

        <div class="profile-stats">
          <div class="pstat">
            <span class="pstat-label">可视化场景</span>
            <span class="pstat-value">{{ formatNumber(stats.sceneCount) }}</span>
          </div>
          <div class="pstat">
            <span class="pstat-label">图表配置</span>
            <span class="pstat-value">{{ formatNumber(stats.chartCount) }}</span>
          </div>
          <div class="pstat">
            <span class="pstat-label">观测条目</span>
            <span class="pstat-value">{{ formatNumber(stats.dataCount) }}</span>
          </div>
        </div>
      </section>

      <section class="card account-summary-card">
        <a href="#profile-wallet" class="summary-tile">
          <span class="summary-label">钱包余额</span>
          <strong>{{ walletBalance }}</strong>
          <span class="summary-link">查看流水</span>
        </a>
        <a href="#profile-membership" class="summary-tile">
          <span class="summary-label">会员状态</span>
          <strong>{{ membershipActive ? '有效' : '未开通' }}</strong>
          <span class="summary-link">{{ membershipExpiresText }}</span>
        </a>
        <router-link to="/user/expert-apply" class="summary-tile">
          <span class="summary-label">专家权限</span>
          <strong>{{ roleLabel === '普通用户' ? '可申请' : roleLabel }}</strong>
          <span class="summary-link">提交或查看申请</span>
        </router-link>
      </section>

      <section id="profile-wallet" class="card wallet-inline-card">
        <h3 class="card-title">钱包与徽章</h3>
        <div class="wallet-inline-body">
          <div class="wallet-balance-box">
            <span>当前海洋币</span>
            <strong>{{ walletBalance }}</strong>
            <p>专家评估奖励、徽章奖励和模拟充值赠币会写入这里。</p>
          </div>
          <div class="badge-inline-list">
            <div v-for="badge in badgeCatalog" :key="String(badge.id)" class="badge-inline" :class="{ unlocked: unlockedBadgeIds.has(String(badge.id)) }">
              <span class="badge-code">{{ badge.code?.slice(0, 1) || 'B' }}</span>
              <div>
                <strong>{{ badge.name }}</strong>
                <p>{{ badge.thresholdCount }} 次评估 · 奖励 {{ badge.rewardCoins }} 币</p>
              </div>
            </div>
            <div v-if="!badgeCatalog.length" class="inline-empty">暂无徽章规则</div>
          </div>
        </div>
        <div class="inline-table">
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
              <tr v-for="tx in walletTransactions" :key="String(tx.id)">
                <td>{{ txType(tx.type) }}</td>
                <td :class="Number(tx.amount) >= 0 ? 'pos' : 'neg'">{{ Number(tx.amount) >= 0 ? '+' : '' }}{{ tx.amount }}</td>
                <td>{{ tx.balanceAfter }}</td>
                <td>{{ tx.remark || '-' }}</td>
                <td>{{ formatTime(tx.createdTime) }}</td>
              </tr>
              <tr v-if="!walletTransactions.length">
                <td colspan="5" class="inline-empty">暂无钱包流水</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="profile-membership" class="card membership-inline-card">
        <h3 class="card-title">会员中心</h3>
        <div class="membership-status-row">
          <div>
            <span>当前状态</span>
            <strong>{{ membershipActive ? '会员有效' : '未开通或已过期' }}</strong>
            <p>到期时间：{{ membershipExpiresAt ? formatTime(membershipExpiresAt) : '-' }}</p>
          </div>
          <router-link to="/user/orders" class="btn-light">查看订单</router-link>
        </div>
        <div class="plan-inline-grid">
          <article v-for="plan in membershipPlans" :key="String(plan.id)" class="plan-inline-card">
            <div class="plan-inline-head">
              <strong>{{ plan.name }}</strong>
              <span>{{ plan.days }} 天</span>
            </div>
            <p>{{ plan.description || '海洋平台会员套餐' }}</p>
            <div class="plan-price">¥{{ Number(plan.priceYuan || 0).toFixed(2) }}</div>
            <button class="btn-primary-solid" type="button" :disabled="membershipPayingId === String(plan.id)" @click="buyMembershipPlan(plan)">
              {{ membershipPayingId === String(plan.id) ? '处理中...' : `模拟付款 · 赠 ${plan.bonusCoins || 0} 币` }}
            </button>
          </article>
          <div v-if="!membershipPlans.length" class="inline-empty">暂无会员套餐</div>
        </div>
      </section>

      <!-- 侧边：快捷入口 -->
      <aside class="card quick-card">
        <h3 class="card-title">快捷入口</h3>
        <ul class="quick-list">
          <li><router-link to="/user/scene-gallery">
            <span class="qicon"><Icon name="scene" :size="16" /></span>
            <span class="qtext">浏览 3D 场景</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/chart-gallery">
            <span class="qicon"><Icon name="chart" :size="16" /></span>
            <span class="qtext">浏览图表画廊</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/ocean-analysis">
            <span class="qicon"><Icon name="brain" :size="16" /></span>
            <span class="qtext">AI 海况分析</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/forum">
            <span class="qicon"><Icon name="signal" :size="16" /></span>
            <span class="qtext">社区讨论</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/my-posts">
            <span class="qicon"><Icon name="user" :size="16" /></span>
            <span class="qtext">我发布的帖子</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/expert-apply">
            <span class="qicon"><Icon name="check" :size="16" /></span>
            <span class="qtext">专家申请</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/wallet">
            <span class="qicon"><Icon name="bolt" :size="16" /></span>
            <span class="qtext">我的钱包</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/membership">
            <span class="qicon"><Icon name="heart" :size="16" /></span>
            <span class="qtext">会员中心</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
          <li><router-link to="/user/orders">
            <span class="qicon"><Icon name="list" :size="16" /></span>
            <span class="qtext">我的订单</span>
            <Icon name="external" :size="13" color="#94a3b8" />
          </router-link></li>
        </ul>
      </aside>

      <!-- 资料：基础信息（只读） -->
      <section class="card info-card">
        <h3 class="card-title">基础信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ user?.username || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">用户角色</span>
            <span class="info-value">{{ roleLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">用户 ID</span>
            <span class="info-value mono">{{ String(user?.id ?? '—') }}</span>
          </div>
        </div>
      </section>

      <!-- 登录/注册管理 -->
      <section class="card security-card">
        <h3 class="card-title">登录与注册管理</h3>
        <div class="auth-manage-grid">
          <form class="manage-form" @submit.prevent="handleSwitchAccount">
            <h4>切换登录账号</h4>
            <p>输入其他账号信息后可直接切换当前登录账号</p>
            <div class="form-item">
              <label>用户名</label>
              <input
                v-model="switchLoginForm.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
                required
              />
            </div>
            <div class="form-item">
              <label>密码</label>
              <input
                v-model="switchLoginForm.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                required
              />
            </div>
            <button type="submit" class="btn-primary-solid" :disabled="switchingAccount">
              {{ switchingAccount ? '切换中...' : '切换登录' }}
            </button>
          </form>

          <form class="manage-form" @submit.prevent="handleRegisterAccount">
            <h4>注册新账号</h4>
            <p>创建新账号后可立即在左侧表单切换登录</p>
            <div class="form-item">
              <label>用户名</label>
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="3-20 个字符"
                required
              />
            </div>
            <div class="form-item">
              <label>密码</label>
              <input
                v-model="registerForm.password"
                type="password"
                placeholder="至少 6 位"
                required
              />
            </div>
            <div class="form-item">
              <label>确认密码</label>
              <input
                v-model="registerConfirmPassword"
                type="password"
                placeholder="再次输入密码"
                required
              />
            </div>
            <div class="form-row">
              <div class="form-item">
                <label>邮箱</label>
                <input v-model="registerForm.email" type="email" placeholder="选填" />
              </div>
              <div class="form-item">
                <label>手机号</label>
                <input v-model="registerForm.phone" type="text" placeholder="选填" />
              </div>
            </div>
            <button type="submit" class="btn-primary-solid" :disabled="registeringAccount">
              {{ registeringAccount ? '注册中...' : '注册账号' }}
            </button>
          </form>
        </div>

        <div class="security-actions">
          <button class="btn-danger" @click="handleLogout">退出当前登录</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Icon from '@/components/Icon.vue';
import { useAuthStore } from '@/store/auth';
import { userApi, accountApi } from '@/utils/api-user';
import { rechargeApi, walletApi, type Badge, type MembershipPlan, type WalletTransaction } from '@/utils/api-expert';
import { message } from '@/utils/message';
import { authApi, type RegisterReq } from '@/utils/api-auth';
import type { LoginReq } from '@/types/auth';

const router = useRouter();
const authStore = useAuthStore();

const user = computed(() => authStore.userInfo);

const roleLabel = computed(() => {
  const r = (user.value?.role || '').toString().toUpperCase();
  if (r === 'ADMIN') return '系统管理员';
  if (r === 'USER') return '普通用户';
  return r || '—';
});

const avatarChar = computed(() => {
  const u = user.value?.username || '';
  return u.charAt(0)?.toUpperCase() || 'U';
});

// 头像加载与上传
const avatarLoadFailed = ref(false);
const avatarUploading = ref(false);
const avatarInputRef = ref<HTMLInputElement | null>(null);

const avatarSrc = computed(() => {
  const url = user.value?.avatar || '';
  if (!url) return '';
  // 后端返回 /uploads/... ，直接相对当前域即可（dev 通过 vite proxy 转发，prod 同源）
  return url;
});

const hasAvatar = computed(() => !!avatarSrc.value && !avatarLoadFailed.value);

const onAvatarError = () => {
  avatarLoadFailed.value = true;
};

const triggerAvatarInput = () => {
  if (avatarUploading.value) return;
  avatarInputRef.value?.click();
};

const handleAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  // 重置 value 让相同文件能再次触发 change
  input.value = '';
  if (!file) return;
  if (!/^image\/(png|jpe?g|gif|webp)$/i.test(file.type)) {
    message.warning('仅支持 PNG/JPG/GIF/WEBP 图片');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('图片大小不能超过 5MB');
    return;
  }
  avatarUploading.value = true;
  try {
    const res = await accountApi.uploadAvatar(file);
    if (res?.avatar) {
      avatarLoadFailed.value = false;
      authStore.updateAvatar(res.avatar);
      message.success('头像更新成功');
    }
  } catch (err) {
    console.error('头像上传失败:', err);
  } finally {
    avatarUploading.value = false;
  }
};

const stats = ref({ sceneCount: 0, chartCount: 0, dataCount: 0 });
const walletBalance = ref(0);
const membershipExpiresAt = ref('');
const walletTransactions = ref<WalletTransaction[]>([]);
const badgeCatalog = ref<Badge[]>([]);
const userBadgeIds = ref<string[]>([]);
const membershipPlans = ref<MembershipPlan[]>([]);
const membershipPayingId = ref('');
const switchingAccount = ref(false);
const registeringAccount = ref(false);

const switchLoginForm = ref<LoginReq>({
  username: '',
  password: '',
});

const registerForm = ref<RegisterReq>({
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
});
const registerConfirmPassword = ref('');

const formatNumber = (n: number) => Number.isFinite(n) ? n.toLocaleString('en-US') : '0';
const formatTime = (v?: string) => v ? v.replace('T', ' ') : '-';

const unlockedBadgeIds = computed(() => new Set(userBadgeIds.value));

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

const membershipActive = computed(() => {
  if (!membershipExpiresAt.value) return false;
  return new Date(membershipExpiresAt.value).getTime() > Date.now();
});

const membershipExpiresText = computed(() => {
  if (!membershipExpiresAt.value) return '开通会员';
  return `到期 ${membershipExpiresAt.value.replace('T', ' ')}`;
});

const loadStats = async () => {
  try {
    const res = await userApi.getUserStats();
    stats.value = {
      sceneCount: Number(res.sceneCount) || 0,
      chartCount: Number(res.chartCount) || 0,
      dataCount: Number(res.dataCount) || 0,
    };
  } catch (e) {
    console.error('加载统计失败:', e);
  }
};

const loadAccountExtras = async () => {
  const [wallet, membership, transactions, badges, myBadges, plans] = await Promise.allSettled([
    walletApi.getMyWallet(true),
    rechargeApi.getMyMembership(true),
    walletApi.getTransactions(20, true),
    walletApi.getBadgeCatalog(true),
    walletApi.getMyBadges(true),
    rechargeApi.listPlans(true),
  ]);
  if (wallet.status === 'fulfilled') walletBalance.value = Number(wallet.value?.balanceCoins) || 0;
  if (membership.status === 'fulfilled') membershipExpiresAt.value = membership.value?.expiresAt || '';
  if (transactions.status === 'fulfilled') walletTransactions.value = transactions.value || [];
  if (badges.status === 'fulfilled') badgeCatalog.value = badges.value || [];
  if (myBadges.status === 'fulfilled') userBadgeIds.value = (myBadges.value || []).map((b: any) => String(b.badgeId));
  if (plans.status === 'fulfilled') membershipPlans.value = plans.value || [];
};

const buyMembershipPlan = async (plan: MembershipPlan) => {
  const ok = window.confirm(`确认使用模拟支付购买「${plan.name}」？`);
  if (!ok) return;
  membershipPayingId.value = String(plan.id);
  try {
    const order = await rechargeApi.createOrder(plan.id);
    await rechargeApi.mockPay(order.id!, 'WECHAT_MOCK');
    message.success('模拟支付成功，会员和赠币已到账');
    await loadAccountExtras();
  } finally {
    membershipPayingId.value = '';
  }
};

const handleLogout = async () => {
  await authStore.logout();
  message.success('已安全退出');
};

const resetRegisterForm = () => {
  registerForm.value = {
    username: '',
    password: '',
    email: '',
    phone: '',
    realName: '',
  };
  registerConfirmPassword.value = '';
};

const handleSwitchAccount = async () => {
  const username = switchLoginForm.value.username.trim();
  const password = switchLoginForm.value.password;
  if (!username || !password) {
    message.warning('请填写用户名和密码');
    return;
  }

  switchingAccount.value = true;
  try {
    await authStore.login({ username, password });
    switchLoginForm.value.password = '';
    await loadStats();
    await loadAccountExtras();
    if (authStore.isAdmin) {
      router.push('/admin/dashboard');
    } else {
      message.success('账号切换成功');
    }
  } catch (error) {
    console.error('切换账号失败:', error);
  } finally {
    switchingAccount.value = false;
  }
};

const handleRegisterAccount = async () => {
  const username = registerForm.value.username?.trim() || '';
  const password = registerForm.value.password || '';
  const confirmPassword = registerConfirmPassword.value || '';

  if (!username || !password) {
    message.warning('用户名和密码不能为空');
    return;
  }
  if (username.length < 3 || username.length > 20) {
    message.warning('用户名长度需为 3-20 个字符');
    return;
  }
  if (password.length < 6) {
    message.warning('密码至少需要 6 位');
    return;
  }
  if (password !== confirmPassword) {
    message.warning('两次输入的密码不一致');
    return;
  }

  registeringAccount.value = true;
  try {
    await authApi.register({
      ...registerForm.value,
      username,
      password,
    });
    message.success('注册成功，可立即切换登录');
    switchLoginForm.value.username = username;
    switchLoginForm.value.password = '';
    resetRegisterForm();
  } catch (error) {
    console.error('注册失败:', error);
  } finally {
    registeringAccount.value = false;
  }
};

onMounted(() => {
  loadStats();
  loadAccountExtras();
});
</script>

<style scoped lang="less">
@accent: #0284c7;
@accent-2: #0ea5e9;
@accent-cyan: #06b6d4;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@ink-4: #94a3b8;
@hairline: #e2e8f0;
@paper: #ffffff;
@bg-soft: #f8fafc;

.user-profile {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  animation: fadeIn 0.5s ease-out;
}

.profile-page-header {
  order: 0;
  width: 100%;
  margin-bottom: 22px;
  padding-bottom: 14px;
  border-bottom: 1px solid @hairline;
}

.profile-page-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.15;
  color: @ink-1;
  font-weight: 800;
  letter-spacing: -0.4px;
  position: relative;
  padding-left: 16px;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 6px;
    bottom: 6px;
    width: 4px;
    border-radius: 2px;
    background: linear-gradient(180deg, #0284c7, #06b6d4);
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.profile-grid {
  order: 1;
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 22px;
  align-items: start;
}

.card {
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 16px;
  padding: 0;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: @ink-2;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  margin: 0;
  padding: 18px 24px;
  border-bottom: 1px solid @hairline;
  background: @bg-soft;
}

/* ========== Profile main card ========== */
.profile-card {
  grid-column: 1 / span 2;
  position: relative;
}

.account-summary-card {
  grid-column: 1 / span 2;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 0;
}

.summary-tile {
  padding: 20px 24px;
  text-decoration: none;
  border-right: 1px solid @hairline;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: background 0.18s ease;

  &:last-child { border-right: none; }

  &:hover {
    background: #f0f9ff;
  }

  .summary-label {
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.8px;
    color: @ink-3;
    text-transform: uppercase;
  }

  strong {
    color: @ink-1;
    font-size: 24px;
    line-height: 1.2;
  }

  .summary-link {
    color: @accent;
    font-size: 12px;
    font-weight: 700;
  }
}

.wallet-inline-card,
.membership-inline-card {
  grid-column: 1 / span 2;
}

.wallet-inline-body {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  padding: 20px 24px;
}

.wallet-balance-box {
  padding: 18px;
  border: 1px solid #bae6fd;
  border-radius: 14px;
  background: linear-gradient(135deg, #f0f9ff, #ecfeff);

  span {
    color: @ink-3;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin: 8px 0;
    color: @ink-1;
    font-size: 34px;
    line-height: 1;
  }

  p {
    margin: 0;
    color: @ink-2;
    font-size: 12.5px;
    line-height: 1.6;
  }
}

.badge-inline-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 10px;
}

.badge-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid @hairline;
  border-radius: 12px;
  background: #f8fbff;
  opacity: 0.62;

  &.unlocked {
    opacity: 1;
    border-color: #7dd3fc;
    background: #f0f9ff;
  }

  .badge-code {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: #fff;
    font-weight: 900;
    background: linear-gradient(135deg, #0284c7, #06b6d4);
  }

  strong {
    color: @ink-1;
    font-size: 13px;
  }

  p {
    margin: 3px 0 0;
    color: @ink-3;
    font-size: 12px;
  }
}

.inline-table {
  padding: 0 24px 20px;
  overflow-x: auto;

  table {
    width: 100%;
    border-collapse: collapse;
  }

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid @hairline;
    color: @ink-2;
    font-size: 12.5px;
    text-align: left;
  }

  th {
    color: @ink-1;
    background: #f8fbff;
    font-weight: 800;
  }

  .pos { color: #16a34a; font-weight: 800; }
  .neg { color: #dc2626; font-weight: 800; }
}

.inline-empty {
  color: @ink-3;
  text-align: center;
  padding: 18px;
}

.membership-status-row {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  border-bottom: 1px solid @hairline;

  span {
    color: @ink-3;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin: 6px 0;
    color: @ink-1;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: @ink-2;
    font-size: 13px;
  }
}

.btn-light {
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid @hairline;
  background: #fff;
  color: @ink-2;
  text-decoration: none;
  font-size: 12.5px;
  font-weight: 800;
}

.plan-inline-grid {
  padding: 20px 24px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.plan-inline-card {
  padding: 16px;
  border: 1px solid @hairline;
  border-radius: 14px;
  background: #fbfdff;

  p {
    min-height: 38px;
    margin: 10px 0;
    color: @ink-2;
    font-size: 12.5px;
    line-height: 1.5;
  }

  .btn-primary-solid {
    margin-top: 10px;
  }
}

.plan-inline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: @ink-1;

  span {
    flex-shrink: 0;
    padding: 3px 8px;
    border-radius: 999px;
    background: #e0f2fe;
    color: @accent;
    font-size: 11px;
    font-weight: 800;
  }
}

.plan-price {
  color: @ink-1;
  font-size: 24px;
  font-weight: 900;
}

.profile-banner {
  height: 110px;
  background:
    radial-gradient(circle at 0% 50%, rgba(255, 255, 255, 0.25), transparent 50%),
    radial-gradient(circle at 100% 50%, rgba(255, 255, 255, 0.18), transparent 50%),
    linear-gradient(110deg, @accent 0%, @accent-2 60%, @accent-cyan 100%);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
    background-size: 24px 24px;
  }
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 28px 24px;
  margin-top: -50px;
  position: relative;
}

.profile-avatar {
  width: 86px;
  height: 86px;
  border-radius: 22px;
  background: linear-gradient(135deg, #0284c7, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -1px;
  box-shadow: 0 12px 28px -8px rgba(2, 132, 199, 0.45);
  border: 4px solid #fff;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &.has-image {
    background: #e2e8f0;
  }

  img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: inherit;
  }

  .avatar-mask {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    background: rgba(15, 23, 42, 0.6);
    color: #fff;
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.4px;
    opacity: 0;
    transition: opacity 0.18s ease;
    border-radius: inherit;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 16px 32px -10px rgba(2, 132, 199, 0.55);

    .avatar-mask { opacity: 1; }
  }

  &:focus-visible {
    outline: 3px solid rgba(2, 132, 199, 0.45);
    outline-offset: 2px;
  }

  &.uploading .avatar-mask {
    opacity: 1;
    background: rgba(2, 132, 199, 0.7);
  }
}

.avatar-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.profile-meta {
  flex: 1;
  padding-top: 50px;
}

.profile-name {
  font-size: 22px;
  font-weight: 800;
  color: @ink-1;
  margin: 0 0 8px;
  letter-spacing: -0.4px;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  letter-spacing: 0.2px;

  &.tag-role {
    background: rgba(2, 132, 199, 0.08);
    color: @accent;
    border: 1px solid rgba(2, 132, 199, 0.18);
  }
  &.tag-id {
    background: @bg-soft;
    color: @ink-3;
    border: 1px solid @hairline;
    font-family: 'JetBrains Mono', 'Consolas', monospace;
  }
  &.tag-status {
    background: rgba(34, 197, 94, 0.08);
    color: #16a34a;
    border: 1px solid rgba(34, 197, 94, 0.22);

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #22c55e;
      box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
    }
  }
}

.btn-logout {
  align-self: flex-start;
  margin-top: 50px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  color: @ink-2;
  border: 1px solid @hairline;
  border-radius: 9px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:hover {
    background: #fef2f2;
    border-color: #fecaca;
    color: #dc2626;
  }
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid @hairline;
}

.pstat {
  padding: 22px 28px;
  border-right: 1px solid @hairline;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:last-child { border-right: none; }
}

.pstat-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: @ink-4;
}

.pstat-value {
  font-size: 26px;
  font-weight: 800;
  color: @ink-1;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}

/* ========== Quick actions ========== */
.quick-card {
  align-self: stretch;
}

.quick-list {
  list-style: none;
  padding: 12px 0;
  margin: 0;
}

.quick-list a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  text-decoration: none;
  color: @ink-2;
  font-size: 13.5px;
  font-weight: 500;
  transition: background 0.18s;

  .qicon {
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: @bg-soft;
    color: @accent;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    flex-shrink: 0;
  }

  .qtext { flex: 1; }

  &:hover {
    background: rgba(2, 132, 199, 0.04);
    color: @ink-1;

    .qicon {
      background: @accent;
      color: #fff;
    }
  }
}

/* ========== Info card ========== */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  padding: 6px 24px 18px;
}

.info-item {
  padding: 14px 0;
  border-bottom: 1px solid @hairline;
  display: flex;
  flex-direction: column;
  gap: 4px;

  &:last-child { border-bottom: none; }
}

.info-label {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: @ink-4;
  text-transform: uppercase;
}

.info-value {
  font-size: 14px;
  color: @ink-1;
  font-weight: 500;

  &.mono {
    font-family: 'JetBrains Mono', 'Consolas', monospace;
    font-size: 13px;
    color: @ink-2;
  }
}

/* ========== Security ========== */
.security-card { grid-column: 1 / span 2; }

.auth-manage-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 20px 24px 18px;
}

.manage-form {
  border: 1px solid @hairline;
  border-radius: 12px;
  padding: 16px;
  background: #fbfdff;

  h4 {
    margin: 0 0 6px;
    font-size: 15px;
    color: @ink-1;
    font-weight: 700;
  }

  p {
    margin: 0 0 14px;
    font-size: 12.5px;
    color: @ink-3;
    line-height: 1.5;
  }
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.form-item {
  margin-bottom: 12px;

  label {
    display: block;
    margin-bottom: 6px;
    font-size: 12px;
    color: @ink-3;
    font-weight: 600;
  }
}

.btn-primary-solid {
  width: 100%;
  border: none;
  border-radius: 9px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 8px 18px -8px rgba(2, 132, 199, 0.6);
  }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
  }
}

.security-actions {
  padding: 0 24px 20px;
  display: flex;
  justify-content: flex-end;
}

.btn-danger {
  padding: 8px 18px;
  background: #fff;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  white-space: nowrap;

  &:hover {
    background: #fef2f2;
    border-color: #f87171;
  }
}

/* ========== Responsive ========== */
@media (max-width: 960px) {
  .profile-page-title { font-size: 24px; }
  .profile-grid { grid-template-columns: 1fr; }
  .profile-card { grid-column: auto; }
  .account-summary-card { grid-column: auto; grid-template-columns: 1fr; }
  .wallet-inline-card,
  .membership-inline-card { grid-column: auto; }
  .wallet-inline-body { grid-template-columns: 1fr; padding: 16px; }
  .inline-table { padding: 0 16px 16px; }
  .membership-status-row { flex-direction: column; align-items: flex-start; padding: 16px; }
  .plan-inline-grid { padding: 16px; }
  .summary-tile { border-right: none; border-bottom: 1px solid @hairline; }
  .summary-tile:last-child { border-bottom: none; }
  .security-card { grid-column: auto; }
  .profile-head { flex-direction: column; align-items: flex-start; gap: 12px; padding-bottom: 20px; }
  .profile-meta { padding-top: 8px; }
  .btn-logout { margin-top: 0; align-self: flex-end; }
  .profile-stats { grid-template-columns: 1fr; }
  .pstat { border-right: none; border-bottom: 1px solid @hairline; }
  .pstat:last-child { border-bottom: none; }
  .info-grid { grid-template-columns: 1fr; }
  .info-item:last-child { border-bottom: none; }
  .auth-manage-grid { grid-template-columns: 1fr; padding: 16px; }
  .form-row { grid-template-columns: 1fr; }
  .security-actions { padding: 0 16px 16px; }
}
</style>
