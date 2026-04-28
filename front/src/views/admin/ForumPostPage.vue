<template>
  <div class="forum-post-page">
    <PageHeader title="帖子管理">
      <template #extra>
        <div style="display: flex; gap: 12px;">
          <select v-model="filterStatus" class="filter-select" @change="loadData">
            <option value="">全部状态</option>
            <option :value="0">草稿</option>
            <option :value="1">已发布</option>
            <option :value="2">已关闭</option>
          </select>
          <select v-model="filterCategory" class="filter-select" @change="loadData">
            <option value="">全部分类</option>
            <option value="GENERAL">普通讨论</option>
            <option value="QUESTION">问题求助</option>
            <option value="SHARE">经验分享</option>
            <option value="NEWS">新闻资讯</option>
          </select>
          <select v-model="filterReliabilityStatus" class="filter-select" @change="loadData">
            <option value="">全部评估状态</option>
            <option :value="0">未评选</option>
            <option :value="1">评估中/未通过</option>
            <option :value="2">已认证</option>
          </select>
          <select v-model="filterEvaluationCompleted" class="filter-select" @change="loadData">
            <option value="">评估完成度</option>
            <option value="1">已完成评估</option>
            <option value="0">未完成评估</option>
          </select>
          <select v-model="filterReliabilityTrusted" class="filter-select" @change="loadData">
            <option value="">可信结果</option>
            <option value="1">可信</option>
            <option value="0">不可信</option>
          </select>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索标题或内容..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-default" @click="handleSearch">搜索</button>
        </div>
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
        <!-- 置顶开关 -->
        <template #isTop="{ row }">
          <label class="switch">
            <input
              type="checkbox"
              :checked="row.isTop === 1"
              @change="handleToggleTop(row, $event)"
            />
            <span class="slider"></span>
          </label>
        </template>

        <!-- 精华开关 -->
        <template #isEssence="{ row }">
          <label class="switch">
            <input
              type="checkbox"
              :checked="row.isEssence === 1"
              @change="handleToggleEssence(row, $event)"
            />
            <span class="slider"></span>
          </label>
        </template>

        <!-- 状态Badge -->
        <template #status="{ row }">
          <span :class="['status-badge', getStatusClass(row.status)]">
            {{ getStatusText(row.status) }}
          </span>
        </template>

        <template #reliability="{ row }">
          <div class="reliability-cell">
            <span :class="['reliability-badge', getReliabilityClass(row)]">
              {{ getReliabilityText(row) }}
            </span>
            <span v-if="row.postType === 'DATA_ANALYSIS'" class="reliability-meta">
              {{ row.evaluationCount || 0 }}/3 · {{ row.reliabilityScore || 0 }}分
            </span>
          </div>
        </template>

        <!-- 标题列 - 可点击 -->
        <template #title="{ row }">
          <span class="title-link" @click="handleTitleClick(row)">
            {{ row.title }}
          </span>
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

    <!-- 删除确认 Modal -->
    <Modal
      v-model:visible="deleteModalVisible"
      title="确认删除"
      @confirm="confirmDeletePost"
    >
      <div style="padding: 20px 0; color: #334155;">
        <p style="font-size: 16px; margin: 0;">确定要删除这条帖子吗？删除后无法恢复，该帖子下的所有评论也将被删除。</p>
      </div>
    </Modal>

    <!-- 帖子详情 Modal -->
    <Modal
      v-model:visible="detailModalVisible"
      title="帖子详情"
      :show-footer="false"
      class="post-detail-modal"
    >
      <div v-if="detailLoading" class="detail-loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="postDetail" class="post-detail-content">
        <!-- 帖子信息 -->
        <div class="detail-header">
          <div class="detail-badges">
            <span v-if="postDetail.isTop === 1" class="top-badge">置顶</span>
            <span v-if="postDetail.isEssence === 1" class="essence-badge">精华</span>
            <span class="category-badge">{{ getCategoryLabel(postDetail.category) }}</span>
            <span :class="['status-badge', getStatusClass(postDetail.status)]">
              {{ getStatusText(postDetail.status) }}
            </span>
          </div>
          <h2 class="detail-title">{{ postDetail.title }}</h2>
          <div class="detail-meta">
            <span>作者 {{ postDetail.authorName }}</span>
            <span>发布时间 {{ postDetail.createdTime }}</span>
            <span>浏览 {{ postDetail.viewCount || 0 }}</span>
            <span>点赞 {{ postDetail.likeCount || 0 }}</span>
            <span>评论 {{ postDetail.commentCount || 0 }}</span>
          </div>
        </div>

        <!-- 帖子内容 -->
        <div class="detail-body">
          <div class="detail-content-text" v-html="formatContent(postDetail.content)"></div>
        </div>

        <!-- 评论列表 -->
        <div class="detail-comments">
          <h3 class="comments-title">评论列表 ({{ comments.length }})</h3>
          <div v-if="comments.length === 0" class="no-comments">
            <p>暂无评论</p>
          </div>
          <div v-else class="comments-list">
            <div
              v-for="comment in comments"
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <span class="comment-author">{{ comment.userName }}</span>
                <span class="comment-time">{{ formatTime(comment.createdTime) }}</span>
              </div>
              <div class="comment-content">
                <span v-if="comment.replyToUserName" class="reply-to">
                  @{{ comment.replyToUserName }}
                </span>
                {{ comment.content }}
              </div>
              <div class="comment-meta">
                <span>点赞 {{ comment.likeCount || 0 }}</span>
              </div>
              <!-- 子评论 -->
              <div v-if="comment.children && comment.children.length > 0" class="comment-children">
                <div
                  v-for="child in comment.children"
                  :key="child.id"
                  class="comment-item is-reply"
                >
                  <div class="comment-header">
                    <span class="comment-author">{{ child.userName }}</span>
                    <span class="comment-time">{{ formatTime(child.createdTime) }}</span>
                  </div>
                  <div class="comment-content">
                    <span v-if="child.replyToUserName" class="reply-to">
                      @{{ child.replyToUserName }}
                    </span>
                    {{ child.content }}
                  </div>
                  <div class="comment-meta">
                    <span>点赞 {{ child.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import DataTable from '@/components/DataTable.vue';
import Pagination from '@/components/Pagination.vue';
import Modal from '@/components/Modal.vue';
import { forumPostApi, type ForumPost, type ForumComment } from '@/utils/api-admin';
import { message } from '@/utils/message';

const columns = [
  { key: 'id', title: 'ID', style: { width: '70px', textAlign: 'center' } },
  { 
    key: 'title', 
    title: '标题',
    style: { minWidth: '180px', maxWidth: '220px' }
  },
  { key: 'authorName', title: '作者', style: { width: '100px' } },
  { 
    key: 'category', 
    title: '分类',
    formatter: (value: string) => {
      const map: Record<string, string> = {
        GENERAL: '普通讨论',
        QUESTION: '问题求助',
        SHARE: '经验分享',
        NEWS: '新闻资讯',
      };
      return map[value] || value;
    },
    style: { width: '90px' }
  },
  { 
    key: 'isTop', 
    title: '置顶',
    style: { width: '70px', textAlign: 'center' }
  },
  { 
    key: 'isEssence', 
    title: '精华',
    style: { width: '70px', textAlign: 'center' }
  },
  { 
    key: 'status', 
    title: '状态',
    style: { width: '90px', textAlign: 'center' }
  },
  {
    key: 'reliability',
    title: '专家评估',
    style: { width: '130px', textAlign: 'center' }
  },
  { key: 'viewCount', title: '浏览量', style: { width: '90px', textAlign: 'center' } },
  { key: 'likeCount', title: '点赞数', style: { width: '90px', textAlign: 'center' } },
  { key: 'commentCount', title: '评论数', style: { width: '90px', textAlign: 'center' } },
  { key: 'createdTime', title: '创建时间', style: { width: '160px' } },
];

const actions = [
  { key: 'delete', label: '删除', type: 'danger' as const },
];

const tableData = ref<ForumPost[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterStatus = ref<number | ''>('');
const filterCategory = ref<string>('');
const filterReliabilityStatus = ref<number | ''>('');
const filterEvaluationCompleted = ref<string>('');
const filterReliabilityTrusted = ref<string>('');
const keyword = ref('');
const deleteModalVisible = ref(false);
const deletingPostId = ref<string | null>(null);
const detailModalVisible = ref(false);
const postDetail = ref<ForumPost | null>(null);
const comments = ref<ForumComment[]>([]);
const detailLoading = ref(false);
const REQUIRED_EVALUATIONS = 3;
const RELIABILITY_PASS_SCORE = 70;

const loadData = async () => {
  loading.value = true;
  try {
    const res = await forumPostApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: filterStatus.value === '' ? undefined : filterStatus.value,
      category: filterCategory.value || undefined,
      reliabilityStatus: filterReliabilityStatus.value === '' ? undefined : filterReliabilityStatus.value,
      evaluationCompleted:
        filterEvaluationCompleted.value === '' ? undefined : filterEvaluationCompleted.value === '1',
      reliabilityTrusted:
        filterReliabilityTrusted.value === '' ? undefined : filterReliabilityTrusted.value === '1',
      keyword: keyword.value || undefined,
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
  pageNum.value = 1;
  loadData();
};

const handleSearch = () => {
  pageNum.value = 1;
  loadData();
};

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已关闭',
  };
  return map[status] || '未知';
};

const getStatusClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'draft',
    1: 'published',
    2: 'closed',
  };
  return map[status] || '';
};

const getReliabilityText = (post?: ForumPost) => {
  if (post?.postType !== 'DATA_ANALYSIS') return '不适用';
  const count = Number(post?.evaluationCount ?? 0);
  const score = Number(post?.reliabilityScore ?? 0);
  if (post?.reliabilityStatus === 2) return '已认证';
  if (count >= REQUIRED_EVALUATIONS) return score >= RELIABILITY_PASS_SCORE ? '已认证' : '未通过';
  if (count > 0) return `评估中 ${count}/${REQUIRED_EVALUATIONS}`;
  return '未评选';
};

const getReliabilityClass = (post?: ForumPost) => {
  if (post?.postType !== 'DATA_ANALYSIS') return 'none';
  const count = Number(post?.evaluationCount ?? 0);
  const score = Number(post?.reliabilityScore ?? 0);
  if (post?.reliabilityStatus === 2) return 'certified';
  if (count >= REQUIRED_EVALUATIONS && score < RELIABILITY_PASS_SCORE) return 'failed';
  if (post?.reliabilityStatus === 1 || count > 0) return 'progress';
  return 'none';
};

const handleToggleTop = async (row: ForumPost, event: Event) => {
  const target = event.target as HTMLInputElement;
  const newTopValue = target.checked ? 1 : 0;
  
  // 先更新UI，如果失败则回滚
  const originalValue = row.isTop;
  row.isTop = newTopValue;
  
  try {
    await forumPostApi.setTop(row.id!, newTopValue);
    message.success(newTopValue === 1 ? '已置顶' : '已取消置顶');
  } catch (error: any) {
    // 回滚UI状态
    row.isTop = originalValue;
    target.checked = originalValue === 1;
    console.error('操作失败:', error);
    message.error(error?.message || '操作失败，请稍后重试');
  }
};

const handleToggleEssence = async (row: ForumPost, event: Event) => {
  const target = event.target as HTMLInputElement;
  const newEssenceValue = target.checked ? 1 : 0;
  
  // 先更新UI，如果失败则回滚
  const originalValue = row.isEssence;
  row.isEssence = newEssenceValue;
  
  try {
    await forumPostApi.setEssence(row.id!, newEssenceValue);
    message.success(newEssenceValue === 1 ? '已设为精华' : '已取消精华');
  } catch (error: any) {
    // 回滚UI状态
    row.isEssence = originalValue;
    target.checked = originalValue === 1;
    console.error('操作失败:', error);
    message.error(error?.message || '操作失败，请稍后重试');
  }
};

const handleAction = async (action: any, row: ForumPost) => {
  if (action.key === 'delete') {
    deletingPostId.value = row.id != null ? String(row.id) : null;
    deleteModalVisible.value = true;
  }
};

const confirmDeletePost = async () => {
  if (!deletingPostId.value) return;

  try {
    await forumPostApi.delete(deletingPostId.value);
    message.success('删除成功');
    loadData();
    deletingPostId.value = null;
  } catch (error: any) {
    console.error('删除失败:', error);
    message.error(error?.message || '删除失败，请稍后重试');
  }
};

const handleTitleClick = async (row: ForumPost) => {
  if (!row.id) return;
  
  detailModalVisible.value = true;
  detailLoading.value = true;
  postDetail.value = null;
  comments.value = [];

  try {
    // 并行加载帖子详情和评论
    const [postData, commentsData] = await Promise.all([
      forumPostApi.getById(row.id),
      forumPostApi.getComments(row.id),
    ]);
    postDetail.value = postData;
    comments.value = commentsData;
  } catch (error: any) {
    console.error('加载详情失败:', error);
    message.error(error?.message || '加载详情失败，请稍后重试');
  } finally {
    detailLoading.value = false;
  }
};

const getCategoryLabel = (category?: string) => {
  const map: Record<string, string> = {
    GENERAL: '普通讨论',
    QUESTION: '问题求助',
    SHARE: '经验分享',
    NEWS: '新闻资讯',
  };
  return map[category || ''] || category || '未知';
};

const formatContent = (content?: string) => {
  if (!content) return '';
  // 简单的格式化：换行、代码块
  return content
    .replace(/\n/g, '<br>')
    .replace(/`(.+?)`/g, '<code>$1</code>');
};

const formatTime = (time?: string) => {
  if (!time) return '';
  return time;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="less">
.forum-post-page {
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

.filter-select,
.search-input {
  padding: 8px 12px;
  background: rgba(15, 20, 45, 0.6);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 6px;
  color: rgba(224, 242, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
  outline: none;

  &:focus {
    border-color: rgba(102, 126, 234, 0.6);
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.15);
  }

  &::placeholder {
    color: rgba(224, 242, 255, 0.5);
  }
}

.filter-select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23fff' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
  color-scheme: dark;
  min-width: 120px;

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
}

.search-input {
  min-width: 200px;
}

// 开关样式
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
  user-select: none;

  input {
    opacity: 0;
    width: 0;
    height: 0;

    &:checked + .slider {
      background: linear-gradient(135deg, #4a90e2, #00d4ff);
      box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.3);

      &::before {
        transform: translateX(20px);
      }
    }

    &:focus + .slider {
      box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.2);
    }
  }

  .slider {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid rgba(102, 126, 234, 0.3);
    border-radius: 24px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;

    &::before {
      position: absolute;
      content: '';
      height: 18px;
      width: 18px;
      left: 2px;
      bottom: 2px;
      background: #fff;
      border-radius: 50%;
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    &:hover {
      border-color: rgba(102, 126, 234, 0.5);
    }
  }
}

// 状态Badge样式
.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  
  &.draft {
    background: rgba(255, 193, 7, 0.2);
    color: #ffc107;
    border: 1px solid rgba(255, 193, 7, 0.3);
  }
  
  &.published {
    background: rgba(76, 175, 80, 0.2);
    color: #4caf50;
    border: 1px solid rgba(76, 175, 80, 0.3);
  }
  
  &.closed {
    background: rgba(158, 158, 158, 0.2);
    color: #9e9e9e;
    border: 1px solid rgba(158, 158, 158, 0.3);
  }
}

.reliability-cell {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.reliability-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 62px;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;

  &.none {
    background: #f1f5f9;
    color: #475569;
    border: 1px solid #dbe8f4;
  }

  &.progress {
    background: #fef3c7;
    color: #92400e;
    border: 1px solid #fde68a;
  }

  &.failed {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fecaca;
  }

  &.certified {
    background: #e0f2fe;
    color: #0369a1;
    border: 1px solid #bae6fd;
  }
}

.reliability-meta {
  color: #475569;
  font-size: 11px;
}

// 标题链接样式
.title-link {
  color: rgba(102, 126, 234, 0.9);
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  
  &:hover {
    color: rgba(102, 126, 234, 1);
    text-decoration: underline;
  }
}

// 优化表格布局
:deep(.data-table) {
  .table-container {
    table {
      tbody {
        td {
          // 标题列样式优化
          &:nth-child(2) {
            font-weight: 500;
            color: rgba(224, 242, 255, 0.95);
            
            // 标题文本溢出处理
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          // 数字列样式（浏览量、点赞数、评论数）
          &:nth-child(9),
          &:nth-child(10),
          &:nth-child(11) {
            font-weight: 500;
            color: rgba(102, 126, 234, 0.9);
            font-family: 'Courier New', monospace;
          }
        }
      }
    }
  }
}

// 详情Modal样式
.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(224, 242, 255, 0.9);
  
  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid rgba(102, 126, 234, 0.3);
    border-top-color: rgba(102, 126, 234, 1);
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 16px;
  }
  
  p {
    margin: 0;
    font-size: 14px;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.post-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
  
  // 滚动条样式
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(15, 20, 45, 0.3);
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(102, 126, 234, 0.5);
    border-radius: 3px;
    
    &:hover {
      background: rgba(102, 126, 234, 0.7);
    }
  }
}

.detail-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  
  .detail-badges {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    flex-wrap: wrap;
    
    .top-badge {
      display: inline-block;
      padding: 4px 10px;
      background: rgba(255, 193, 7, 0.2);
      color: #ffc107;
      border: 1px solid rgba(255, 193, 7, 0.3);
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    
    .essence-badge {
      display: inline-block;
      padding: 4px 10px;
      background: rgba(255, 152, 0, 0.2);
      color: #ff9800;
      border: 1px solid rgba(255, 152, 0, 0.3);
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    
    .category-badge {
      display: inline-block;
      padding: 4px 10px;
      background: rgba(102, 126, 234, 0.2);
      color: rgba(102, 126, 234, 0.9);
      border: 1px solid rgba(102, 126, 234, 0.3);
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
  }
  
  .detail-title {
    font-size: 24px;
    font-weight: 600;
    color: rgba(224, 242, 255, 0.95);
    margin: 0 0 16px 0;
    line-height: 1.4;
  }
  
  .detail-meta {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    font-size: 14px;
    color: rgba(224, 242, 255, 0.7);
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.detail-body {
  margin-bottom: 32px;
  
  .detail-content-text {
    color: rgba(224, 242, 255, 0.9);
    line-height: 1.8;
    font-size: 15px;
    white-space: pre-wrap;
    word-break: break-word;
    
    code {
      background: rgba(102, 126, 234, 0.2);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      font-size: 14px;
      color: rgba(102, 126, 234, 1);
    }
  }
}

.detail-comments {
  .comments-title {
    font-size: 18px;
    font-weight: 600;
    color: rgba(224, 242, 255, 0.95);
    margin: 0 0 16px 0;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  }
  
  .no-comments {
    text-align: center;
    padding: 40px 20px;
    color: rgba(224, 242, 255, 0.5);
    font-size: 14px;
  }
  
  .comments-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  .comment-item {
    padding: 16px;
    background: rgba(15, 20, 45, 0.4);
    border: 1px solid rgba(102, 126, 234, 0.2);
    border-radius: 8px;
    transition: all 0.2s;
    
    &:hover {
      border-color: rgba(102, 126, 234, 0.4);
      background: rgba(15, 20, 45, 0.5);
    }
    
    &.is-reply {
      margin-left: 32px;
      background: rgba(15, 20, 45, 0.3);
      border-color: rgba(102, 126, 234, 0.15);
    }
    
    .comment-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .comment-author {
        font-weight: 500;
        color: rgba(102, 126, 234, 0.9);
        font-size: 14px;
      }
      
      .comment-time {
        font-size: 12px;
        color: rgba(224, 242, 255, 0.5);
      }
    }
    
    .comment-content {
      color: rgba(224, 242, 255, 0.9);
      line-height: 1.6;
      margin-bottom: 8px;
      word-break: break-word;
      
      .reply-to {
        color: rgba(102, 126, 234, 0.9);
        font-weight: 500;
        margin-right: 4px;
      }
    }
    
    .comment-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: rgba(224, 242, 255, 0.6);
    }
    
    .comment-children {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid rgba(102, 126, 234, 0.1);
    }
  }
}

// 详情Modal宽度覆盖
:deep(.post-detail-modal) {
  .modal-container {
    max-width: 900px;
  }
}

/* 浅色主色覆盖 */
.filter-select,
.search-input {
  background: #ffffff;
  border: 1px solid #dbe8f4;
  color: #334155;

  &:focus {
    border-color: rgba(2, 132, 199, 0.55);
    box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.12);
  }

  &::placeholder {
    color: #94a3b8;
  }
}

.filter-select {
  appearance: menulist !important;
  -webkit-appearance: menulist !important;
  -moz-appearance: menulist !important;
  background-image: none !important;
  padding-right: 12px !important;
  color-scheme: light;

  option {
    background: #ffffff !important;
    color: #0f172a !important;
  }

  option:checked {
    background: #e0f2fe !important;
    color: #0c4a6e !important;
  }
}

.switch {
  input {
    &:checked + .slider {
      background: linear-gradient(135deg, #0284c7, #06b6d4);
      box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.2);
    }

    &:focus + .slider {
      box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.16);
    }
  }

  .slider {
    background: #e2e8f0;
    border: 1px solid #cbd5e1;
  }
}

.title-link {
  color: #0284c7;
  &:hover { color: #0369a1; }
}

:deep(.data-table .table-container table tbody td:nth-child(2)) {
  color: #0f172a;
}

:deep(.data-table .table-container table tbody td:nth-child(9)),
:deep(.data-table .table-container table tbody td:nth-child(10)),
:deep(.data-table .table-container table tbody td:nth-child(11)) {
  color: #0284c7;
}

.detail-loading {
  color: #334155;
  .loading-spinner {
    border: 3px solid rgba(2, 132, 199, 0.2);
    border-top-color: #0284c7;
  }
}

.post-detail-content {
  &::-webkit-scrollbar-track { background: #eff6ff; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 116, 139, 0.45); }
}

.detail-header {
  border-bottom: 1px solid #dbe8f4;
  .detail-title { color: #0f172a; }
  .detail-meta { color: #334155; }
}

.detail-body .detail-content-text {
  color: #334155;
  code {
    background: #e0f2fe;
    color: #0c4a6e;
  }
}

.detail-comments {
  .comments-title {
    color: #0f172a;
    border-bottom: 1px solid #dbe8f4;
  }
  .no-comments { color: #475569; }
}

.detail-comments .comment-item {
  background: #f8fbff;
  border: 1px solid #dbe8f4;

  &:hover {
    border-color: #bae6fd;
    background: #f0f9ff;
  }

  &.is-reply {
    background: #f1f5f9;
    border-color: #e2e8f0;
  }

  .comment-header .comment-author { color: #0369a1; }
  .comment-header .comment-time { color: #475569; }

  .comment-content {
    color: #334155;
    .reply-to { color: #0284c7; }
  }

  .comment-meta { color: #334155; }
  .comment-children { border-top: 1px solid #dbe8f4; }
}
</style>

