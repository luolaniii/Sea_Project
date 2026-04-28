<template>
  <div class="my-posts">
    <PageHeader title="我的帖子" description="管理您发布的帖子">
      <template #extra>
        <button class="btn btn-primary" @click="goToForum">
          <Icon name="edit" :size="14" /> 发布新帖子
        </button>
      </template>
    </PageHeader>

    <!-- 筛选栏 -->
    <div class="filter-bar card">
      <div class="filter-left">
        <button
          v-for="cat in categories"
          :key="cat.value"
          :class="['filter-btn', { active: selectedCategory === cat.value }]"
          @click="handleCategoryChange(cat.value)"
        >
          {{ cat.label }}
        </button>
      </div>
      <div class="filter-right">
        <select v-model="selectedPostType" class="search-input" @change="handleSearch">
          <option value="">全部类型</option>
          <option value="TOPIC_DISCUSSION">主题讨论</option>
          <option value="DATA_ANALYSIS">数据分析</option>
        </select>
        <select v-model="selectedReliabilityStatus" class="search-input" @change="handleSearch">
          <option value="">全部可靠性</option>
          <option value="0">未评选</option>
          <option value="1">评估中/未通过</option>
          <option value="2">已认证</option>
        </select>
        <select v-model="selectedEvaluationCompleted" class="search-input" @change="handleSearch">
          <option value="">评估完成度</option>
          <option value="1">已完成评估</option>
          <option value="0">未完成评估</option>
        </select>
        <select v-model="selectedReliabilityTrusted" class="search-input" @change="handleSearch">
          <option value="">可信结果</option>
          <option value="1">可信</option>
          <option value="0">不可信</option>
        </select>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索我的帖子..."
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <button class="btn btn-default" @click="handleSearch">搜索</button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="posts.length === 0" class="text-center">
      <div class="empty-state">
        <div class="empty-icon"><Icon name="edit" :size="42" color="rgba(0, 212, 255, 0.5)" /></div>
        <p>您还没有发布过帖子</p>
        <button class="btn btn-primary" @click="goToForum">去发布</button>
      </div>
    </div>
    <div v-else class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card card">
        <div class="post-header">
          <span v-if="post.isEssence" class="essence-badge">精华</span>
          <span v-if="post.isTop" class="top-badge">置顶</span>
          <span :class="['status-badge', getStatusClass(post.status)]">
            {{ getStatusText(post.status) }}
          </span>
          <span :class="['status-badge', post.postType === 'DATA_ANALYSIS' ? 'status-analysis' : 'status-topic']">
            {{ post.postType === 'DATA_ANALYSIS' ? '数据分析' : '主题讨论' }}
          </span>
          <span v-if="post.postType === 'DATA_ANALYSIS'" :class="['status-badge', getReliabilityClass(post)]">
            {{ getReliabilityText(post) }}
          </span>
          <h3 class="post-title" @click="goToDetail(post.id!)">{{ post.title }}</h3>
        </div>
        <div class="post-content" v-html="getContentPreview(post.content)"></div>
        <div class="post-footer">
          <div class="post-tags">
            <span
              v-for="tag in parseTags(post.tags)"
              :key="tag"
              class="tag"
            >
              {{ tag }}
            </span>
          </div>
          <div class="post-meta">
            <span class="time">{{ formatTime(post.createdTime) }}</span>
            <span class="views"><Icon name="eye" :size="12" /> {{ post.viewCount || 0 }}</span>
            <span class="likes"><Icon name="heart" :size="12" /> {{ post.likeCount || 0 }}</span>
            <span class="comments"><Icon name="chat" :size="12" /> {{ post.commentCount || 0 }}</span>
          </div>
        </div>
        <div class="post-actions">
          <button class="btn btn-small btn-default" @click="goToDetail(post.id!)">
            查看
          </button>
          <button
            v-if="post.status === 0"
            class="btn btn-small btn-primary"
            @click="handleEdit(post)"
          >
            编辑
          </button>
          <button
            class="btn btn-small btn-danger"
            @click="handleDelete(post.id!)"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadPosts"
      @page-size-change="handlePageSizeChange"
    />

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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import Modal from '@/components/Modal.vue';
import Icon from '@/components/Icon.vue';
import { userApi, type ForumPost } from '@/utils/api-user';
import { message } from '@/utils/message';

const router = useRouter();

const posts = ref<ForumPost[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedCategory = ref<string>('');
const selectedPostType = ref<string>('');
const selectedReliabilityStatus = ref<string>('');
const selectedEvaluationCompleted = ref<string>('');
const selectedReliabilityTrusted = ref<string>('');
const keyword = ref('');
const deleteModalVisible = ref(false);
const deletingPostId = ref<number | null>(null);

const categories = [
  { label: '全部', value: '' },
  { label: '普通讨论', value: 'GENERAL' },
  { label: '问题求助', value: 'QUESTION' },
  { label: '经验分享', value: 'SHARE' },
  { label: '新闻资讯', value: 'NEWS' },
];
const REQUIRED_EVALUATIONS = 3;
const RELIABILITY_PASS_SCORE = 70;

const loadPosts = async () => {
  loading.value = true;
  try {
    const res = await userApi.getMyPosts({
      page: pageNum.value,
      size: pageSize.value,
      category: selectedCategory.value || undefined,
      keyword: keyword.value || undefined,
      postType: selectedPostType.value || undefined,
      reliabilityStatus: selectedReliabilityStatus.value === '' ? undefined : Number(selectedReliabilityStatus.value),
      evaluationCompleted:
        selectedEvaluationCompleted.value === '' ? undefined : selectedEvaluationCompleted.value === '1',
      reliabilityTrusted:
        selectedReliabilityTrusted.value === '' ? undefined : selectedReliabilityTrusted.value === '1',
    });
    posts.value = res.list;
    total.value = res.total;
  } catch (error) {
    console.error('加载帖子失败:', error);
    message.error('加载帖子失败');
  } finally {
    loading.value = false;
  }
};

const handleCategoryChange = (category: string) => {
  selectedCategory.value = category;
  pageNum.value = 1;
  loadPosts();
};

const handleSearch = () => {
  pageNum.value = 1;
  loadPosts();
};

const handlePageSizeChange = () => {
  pageNum.value = 1;
  loadPosts();
};

const goToDetail = (id: string | number) => {
  router.push(`/user/forum/${id}`);
};

const goToForum = () => {
  router.push('/user/forum');
};

const formatTime = (time?: string) => {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString();
};

const getContentPreview = (content?: string) => {
  if (!content) return '';
  // 移除Markdown标记，只显示纯文本预览
  const text = content.replace(/[#*`\[\]]/g, '').trim();
  return text.length > 150 ? text.substring(0, 150) + '...' : text;
};

const parseTags = (tags?: string): string[] => {
  if (!tags) return [];
  try {
    return JSON.parse(tags);
  } catch {
    return tags.split(',').map((t) => t.trim()).filter((t) => t);
  }
};

const getStatusText = (status?: number) => {
  switch (status) {
    case 0:
      return '草稿';
    case 1:
      return '已发布';
    case 2:
      return '已关闭';
    default:
      return '未知';
  }
};

const getStatusClass = (status?: number) => {
  switch (status) {
    case 0:
      return 'status-draft';
    case 1:
      return 'status-published';
    case 2:
      return 'status-closed';
    default:
      return '';
  }
};

const getReliabilityText = (post?: ForumPost) => {
  const count = Number(post?.evaluationCount ?? 0);
  const score = Number(post?.reliabilityScore ?? 0);
  if (post?.reliabilityStatus === 2) return '已认证';
  if (count >= REQUIRED_EVALUATIONS) return score >= RELIABILITY_PASS_SCORE ? '已认证' : '未通过认证';
  if (count > 0) return `评估中 ${count}/${REQUIRED_EVALUATIONS}`;
  return '未评选';
};

const getReliabilityClass = (post?: ForumPost) => {
  const count = Number(post?.evaluationCount ?? 0);
  const score = Number(post?.reliabilityScore ?? 0);
  if (post?.reliabilityStatus === 2) return 'status-certified';
  if (count >= REQUIRED_EVALUATIONS && score < RELIABILITY_PASS_SCORE) return 'status-failed';
  if (post?.reliabilityStatus === 1 || count > 0) return 'status-progress';
  return 'status-none';
};

const handleEdit = (post: ForumPost) => {
  // 跳转到编辑页面或打开编辑对话框
  message.info('编辑功能开发中...');
  // 可以跳转到编辑页面：router.push(`/user/forum/edit/${post.id}`);
};

const handleDelete = (id: number) => {
  deletingPostId.value = id;
  deleteModalVisible.value = true;
};

const confirmDeletePost = async () => {
  if (!deletingPostId.value) return;

  try {
    await userApi.deletePost(deletingPostId.value);
    message.success('删除成功');
    loadPosts();
    deletingPostId.value = null;
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
};

onMounted(() => {
  loadPosts();
});
</script>

<style scoped lang="less">
.my-posts {
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-left {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;

  &:hover {
    background: rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.5);
    color: #fff;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3));
    border-color: rgba(102, 126, 234, 0.6);
    color: #fff;
  }
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  width: 200px;

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }

  &:focus {
    outline: none;
    border-color: rgba(102, 126, 234, 0.6);
    background: rgba(255, 255, 255, 0.08);
  }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.post-card {
  padding: 20px 24px;
  transition: all 0.3s;

  &:hover {
    transform: translateX(4px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.4);
  }
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.top-badge {
  padding: 4px 10px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a6f);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.essence-badge {
  padding: 4px 10px;
  background: linear-gradient(135deg, #feca57, #ff9ff3);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;

  &.status-draft {
    background: linear-gradient(135deg, #95a5a6, #7f8c8d);
  }

  &.status-published {
    background: linear-gradient(135deg, #2ecc71, #27ae60);
  }

  &.status-closed {
    background: linear-gradient(135deg, #e74c3c, #c0392b);
  }

  &.status-topic {
    background: linear-gradient(135deg, #5f72bd, #9b23ea);
  }

  &.status-analysis {
    background: linear-gradient(135deg, #00b894, #00cec9);
  }

  &.status-none {
    background: linear-gradient(135deg, #64748b, #334155);
  }

  &.status-progress {
    background: linear-gradient(135deg, #f59e0b, #d97706);
  }

  &.status-failed {
    background: linear-gradient(135deg, #ef4444, #b91c1c);
  }

  &.status-certified {
    background: linear-gradient(135deg, #0284c7, #06b6d4);
  }
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  flex: 1;
  cursor: pointer;
  transition: color 0.3s;

  &:hover {
    color: rgba(102, 126, 234, 1);
  }
}

.post-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 10px;
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.4);
  border-radius: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  flex-wrap: wrap;
}

.post-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(102, 126, 234, 0.2);
}

.btn-small {
  padding: 6px 12px;
  font-size: 13px;
}

.text-center {
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: rgba(255, 255, 255, 0.6);

  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }

  p {
    margin: 16px 0;
    font-size: 16px;
  }
}

/* 浅色主色调可读性覆盖 */
.filter-btn {
  background: #ffffff;
  border-color: #dbe8f4;
  color: #334155;

  &:hover {
    background: #f0f9ff;
    border-color: #7dd3fc;
    color: #0f172a;
  }

  &.active {
    background: linear-gradient(135deg, #0284c7, #06b6d4);
    border-color: rgba(2, 132, 199, 0.6);
    color: #ffffff;
  }
}

.search-input {
  background: #ffffff;
  border-color: #dbe8f4;
  color: #0f172a;

  &::placeholder {
    color: #94a3b8;
  }

  &:focus {
    border-color: #7dd3fc;
    background: #ffffff;
  }
}

.post-card {
  background: #ffffff;
  border: 1px solid #dbe8f4;
}

.post-card:hover {
  box-shadow: 0 10px 24px -12px rgba(2, 132, 199, 0.35);
}

.post-title {
  color: #0f172a;

  &:hover {
    color: #0369a1;
  }
}

.post-content {
  color: #475569;
}

.tag {
  background: #e0f2fe;
  border-color: #bae6fd;
  color: #0369a1;
}

.post-meta {
  color: #334155;
}

.post-actions {
  border-top-color: #dbe8f4;
}

.text-center,
.empty-state {
  color: #334155;
}

.my-posts .post-meta .time,
.my-posts .post-meta .views,
.my-posts .post-meta .likes,
.my-posts .post-meta .comments,
.my-posts .post-content {
  color: #334155 !important;
}

// 响应式设计
@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-right {
    width: 100%;
  }

  .search-input {
    flex: 1;
  }

  .post-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .post-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>

