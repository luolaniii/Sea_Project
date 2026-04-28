<template>
  <div class="account-page">
    <header class="page-hero">
      <span class="eyebrow">EXPERT APPLICATION</span>
      <h1>专家申请</h1>
      <p>提交专业背景后由管理员审核，通过后账户角色升级为专家，可参与帖子可信度评估。</p>
    </header>

    <section v-if="application?.id" class="status-card" :class="statusClass(application.status)">
      <div>
        <span class="status-label">当前申请状态</span>
        <strong>{{ statusText(application.status) }}</strong>
      </div>
      <p v-if="application.reviewRemark">审核备注：{{ application.reviewRemark }}</p>
      <p v-else-if="application.status === 'APPROVED'">审核已通过。若顶部角色未刷新，请重新登录一次。</p>
      <p v-else-if="application.status === 'PENDING'">申请已提交，请等待管理员审核。</p>
      <p v-else>可以根据驳回原因补充信息后重新提交。</p>
    </section>
    <section v-if="loadError" class="status-card rejected">
      <div>
        <span class="status-label">功能状态</span>
        <strong>专家申请暂不可用</strong>
      </div>
      <p>{{ loadError }}</p>
    </section>

    <section class="form-card">
      <div class="card-head">
        <h2>{{ application?.status === 'PENDING' ? '申请信息' : '填写申请资料' }}</h2>
        <span>建议填写真实机构、职业方向和擅长的数据类型。</span>
      </div>
      <form class="apply-form" @submit.prevent="submit">
        <div class="form-grid">
          <label>
            <span>真实姓名</span>
            <input v-model.trim="form.realName" type="text" placeholder="例如：张三" required />
          </label>
          <label>
            <span>所属机构</span>
            <input v-model.trim="form.organization" type="text" placeholder="高校 / 研究所 / 企业" />
          </label>
          <label>
            <span>专业方向</span>
            <input v-model.trim="form.profession" type="text" placeholder="海洋工程 / 气象 / 数据分析" required />
          </label>
          <label>
            <span>擅长标签</span>
            <input v-model.trim="form.expertiseTags" type="text" placeholder="波浪,风速,潮位,异常检测" />
          </label>
        </div>
        <label class="full">
          <span>申请说明</span>
          <textarea v-model.trim="form.applicationReason" rows="6" placeholder="说明你的专业背景、评估经验，以及能参与哪些海洋数据分析评估。" required></textarea>
        </label>
        <div class="actions">
          <button type="button" class="btn-ghost" @click="loadApplication">刷新状态</button>
          <button type="submit" class="btn-primary" :disabled="!!loadError || submitting || application?.status === 'PENDING'">
            {{ loadError ? '暂不可提交' : submitting ? '提交中...' : application?.status === 'PENDING' ? '审核中' : '提交申请' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { expertApi, type ExpertApplication } from '@/utils/api-expert';
import { message } from '@/utils/message';

const application = ref<ExpertApplication | null>(null);
const submitting = ref(false);
const loadError = ref('');
const form = ref<ExpertApplication>({
  realName: '',
  organization: '',
  profession: '',
  expertiseTags: '',
  applicationReason: '',
});

const statusText = (status?: string) => {
  if (status === 'APPROVED') return '已通过';
  if (status === 'REJECTED') return '已驳回';
  return '待审核';
};

const statusClass = (status?: string) => {
  if (status === 'APPROVED') return 'approved';
  if (status === 'REJECTED') return 'rejected';
  return 'pending';
};

const fillForm = (data?: ExpertApplication | null) => {
  if (!data) return;
  form.value = {
    realName: data.realName || '',
    organization: data.organization || '',
    profession: data.profession || '',
    expertiseTags: data.expertiseTags || '',
    applicationReason: data.applicationReason || '',
  };
};

const loadApplication = async () => {
  loadError.value = '';
  try {
    const data = await expertApi.getMyApplication(true);
    application.value = data || null;
    fillForm(data);
  } catch (error: any) {
    console.error('加载专家申请失败:', error);
    loadError.value = error?.message || '无法读取专家申请数据，请确认后端专家申请表已迁移。';
  }
};

const submit = async () => {
  if (application.value?.status === 'PENDING') return;
  submitting.value = true;
  try {
    await expertApi.submitApplication(form.value);
    message.success('专家申请已提交');
    await loadApplication();
  } finally {
    submitting.value = false;
  }
};

onMounted(loadApplication);
</script>

<style scoped lang="less">
@accent: #0284c7;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@hairline: #dbe8f4;
@paper: #ffffff;

.account-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero,
.form-card,
.status-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid @hairline;
  border-radius: 18px;
  box-shadow: 0 16px 44px -28px rgba(15, 23, 42, 0.35);
}

.page-hero {
  padding: 28px 32px;
  background:
    radial-gradient(circle at 100% 0%, rgba(14, 165, 233, 0.16), transparent 46%),
    linear-gradient(135deg, #ffffff, #f0f9ff);

  h1 {
    margin: 6px 0 8px;
    color: @ink-1;
    font-size: 30px;
  }

  p {
    margin: 0;
    color: @ink-2;
    line-height: 1.7;
  }
}

.eyebrow {
  color: @accent;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 2px;
}

.status-card {
  padding: 18px 22px;
  border-left: 5px solid #f59e0b;

  &.approved { border-left-color: #22c55e; }
  &.rejected { border-left-color: #ef4444; }

  strong {
    display: block;
    margin-top: 4px;
    color: @ink-1;
    font-size: 20px;
  }

  p {
    margin: 10px 0 0;
    color: @ink-2;
  }
}

.status-label {
  color: @ink-3;
  font-size: 12px;
  font-weight: 700;
}

.card-head {
  padding: 20px 24px;
  border-bottom: 1px solid @hairline;

  h2 {
    margin: 0 0 6px;
    color: @ink-1;
    font-size: 18px;
  }

  span {
    color: @ink-3;
    font-size: 13px;
  }
}

.apply-form {
  padding: 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: @ink-2;
  font-size: 13px;
  font-weight: 700;
}

input,
textarea {
  width: 100%;
  border: 1px solid @hairline;
  border-radius: 10px;
  padding: 11px 13px;
  color: @ink-1;
  background: #fbfdff;
  font: inherit;
  outline: none;

  &:focus {
    border-color: #38bdf8;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.12);
  }
}

.full {
  margin-top: 16px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary,
.btn-ghost {
  border-radius: 10px;
  padding: 10px 18px;
  border: 1px solid @hairline;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  color: #fff;
  border: none;
  box-shadow: 0 12px 24px -14px rgba(2, 132, 199, 0.75);

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.btn-ghost {
  background: #fff;
  color: @ink-2;
}

@media (max-width: 760px) {
  .form-grid { grid-template-columns: 1fr; }
  .page-hero { padding: 22px; }
}
</style>
