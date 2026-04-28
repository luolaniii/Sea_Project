<template>
  <div class="forum-list">
    <PageHeader title="论坛" description="分享3D可视化经验，交流海洋数据见解">
      <template #extra>
        <button class="btn btn-primary" @click="showPublishDialog = true">
          <Icon name="edit" :size="14" /> 发布帖子
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
        <select v-model="selectedPostType" class="filter-select" @change="handleSearch">
          <option value="">全部类型</option>
          <option value="TOPIC_DISCUSSION">主题讨论</option>
          <option value="DATA_ANALYSIS">数据分析</option>
        </select>
        <select v-model="selectedReliabilityStatus" class="filter-select" @change="handleSearch">
          <option value="">全部可靠性</option>
          <option value="0">未评选</option>
          <option value="1">评估中/未通过</option>
          <option value="2">已认证</option>
        </select>
        <select v-model="selectedEvaluationCompleted" class="filter-select" @change="handleSearch">
          <option value="">评估完成度</option>
          <option value="1">已完成评估</option>
          <option value="0">未完成评估</option>
        </select>
        <select v-model="selectedReliabilityTrusted" class="filter-select" @change="handleSearch">
          <option value="">可信结果</option>
          <option value="1">可信</option>
          <option value="0">不可信</option>
        </select>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索帖子..."
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <button class="btn btn-default" @click="handleSearch">搜索</button>
      </div>
    </div>

    <!-- 置顶帖子 -->
    <div v-if="topPosts.length > 0" class="top-posts">
      <h3 class="section-title"><Icon name="pin" :size="14" color="#00d4ff" /> 置顶</h3>
      <div class="post-list">
        <div v-for="post in topPosts" :key="post.id" class="post-card card top-post">
          <div class="post-header">
            <span class="top-badge">置顶</span>
            <span v-if="post.isEssence" class="essence-badge">精华</span>
            <span :class="['type-badge', getPostTypeClass(post.postType)]">{{ getPostTypeLabel(post.postType) }}</span>
            <h3 class="post-title" @click="goToDetail(post.id!)">{{ post.title }}</h3>
          </div>
          <div class="post-meta">
            <span class="author"><Icon name="user" :size="12" /> {{ post.authorName }}</span>
            <span class="time">{{ formatTime(post.createdTime) }}</span>
            <span class="views"><Icon name="eye" :size="12" /> {{ post.viewCount || 0 }}</span>
            <span class="likes"><Icon name="heart" :size="12" /> {{ post.likeCount || 0 }}</span>
            <span class="comments"><Icon name="chat" :size="12" /> {{ post.commentCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="posts.length === 0" class="text-center">暂无帖子</div>
    <div v-else class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card card">
        <div class="post-header">
          <span v-if="post.isEssence" class="essence-badge">精华</span>
          <span :class="['type-badge', getPostTypeClass(post.postType)]">{{ getPostTypeLabel(post.postType) }}</span>
          <span v-if="post.postType === 'DATA_ANALYSIS'" :class="['reliability-badge', getReliabilityClass(post)]">
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
            <span class="author"><Icon name="user" :size="12" /> {{ post.authorName }}</span>
            <span class="time">{{ formatTime(post.createdTime) }}</span>
            <span class="views"><Icon name="eye" :size="12" /> {{ post.viewCount || 0 }}</span>
            <span class="likes"><Icon name="heart" :size="12" /> {{ post.likeCount || 0 }}</span>
            <span class="comments"><Icon name="chat" :size="12" /> {{ post.commentCount || 0 }}</span>
          </div>
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

    <!-- 发布帖子对话框 -->
    <div v-if="showPublishDialog" class="dialog-overlay" @click="showPublishDialog = false">
      <div class="dialog-content card" @click.stop>
        <div class="dialog-header">
          <h3>发布帖子</h3>
          <button class="close-btn" @click="showPublishDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="newPost.title" type="text" placeholder="请输入帖子标题" />
          </div>
          <div class="form-group">
            <label>分类</label>
            <select v-model="newPost.category">
              <option v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>内容类型</label>
            <select v-model="newPost.postType">
              <option value="TOPIC_DISCUSSION">主题讨论</option>
              <option value="DATA_ANALYSIS">数据分析</option>
            </select>
          </div>
          <div class="form-group" v-if="newPost.postType === 'DATA_ANALYSIS'">
            <label>分析目标</label>
            <input v-model="newPost.analysisTarget" type="text" placeholder="例如：海况预测 / 波浪风险" />
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea
              v-model="newPost.content"
              rows="10"
              placeholder="请输入帖子内容（支持Markdown格式）"
            ></textarea>
          </div>
          <div class="form-group">
            <label>标签（用逗号分隔）</label>
            <input
              v-model="tagsInput"
              type="text"
              placeholder="例如：3D可视化,海洋数据,技术分享"
            />
          </div>
          <div class="form-group" v-if="newPost.postType === 'DATA_ANALYSIS'">
            <label>成果附件（可选，支持多文件）</label>
            <input type="file" multiple @change="handleFileChange" />
            <div v-if="selectedFiles.length > 0" class="file-list">
              <div v-for="(file, index) in selectedFiles" :key="`${file.name}-${index}`" class="file-item">
                <span>{{ file.name }}</span>
                <button type="button" class="file-remove" @click="removeSelectedFile(index)">移除</button>
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-default" @click="showPublishDialog = false">取消</button>
          <button class="btn btn-primary" @click="handlePublish">发布</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import Icon from '@/components/Icon.vue';
import { userApi, type ForumPost } from '@/utils/api-user';
import { message } from '@/utils/message';
import { useAuthStore } from '@/store/auth';

const router = useRouter();
const authStore = useAuthStore();

const posts = ref<ForumPost[]>([]);
const topPosts = ref<ForumPost[]>([]);
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
const showPublishDialog = ref(false);
const tagsInput = ref('');
const selectedFiles = ref<File[]>([]);

const categories = [
  { label: '全部', value: '' },
  { label: '普通讨论', value: 'GENERAL' },
  { label: '问题求助', value: 'QUESTION' },
  { label: '经验分享', value: 'SHARE' },
  { label: '新闻资讯', value: 'NEWS' },
];

const newPost = ref<Partial<ForumPost>>({
  title: '',
  content: '',
  category: 'GENERAL',
  postType: 'TOPIC_DISCUSSION',
  analysisTarget: '',
});

const loadPosts = async () => {
  loading.value = true;
  try {
    const res = await userApi.getForumPosts({
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

const loadTopPosts = async () => {
  try {
    const topList = await userApi.getTopPosts();
    topPosts.value = topList;
  } catch (error) {
    console.error('加载置顶帖子失败:', error);
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

const getPostTypeLabel = (postType?: string) => {
  return postType === 'DATA_ANALYSIS' ? '数据分析' : '主题讨论';
};

const getPostTypeClass = (postType?: string) => {
  return postType === 'DATA_ANALYSIS' ? 'type-analysis' : 'type-topic';
};

const REQUIRED_EVALUATIONS = 3;
const RELIABILITY_PASS_SCORE = 70;

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
  if (post?.reliabilityStatus === 2) return 'reliability-certified';
  if (count >= REQUIRED_EVALUATIONS && score < RELIABILITY_PASS_SCORE) return 'reliability-failed';
  if (post?.reliabilityStatus === 1 || count > 0) return 'reliability-progress';
  return 'reliability-none';
};

const parseTags = (tags?: string): string[] => {
  if (!tags) return [];
  try {
    return JSON.parse(tags);
  } catch {
    return tags.split(',').map((t) => t.trim()).filter((t) => t);
  }
};

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;
  selectedFiles.value = Array.from(input.files);
};

const removeSelectedFile = (index: number) => {
  selectedFiles.value.splice(index, 1);
};

const handlePublish = async () => {
  if (!newPost.value.title || !newPost.value.content) {
    message.error('请填写标题和内容');
    return;
  }

  try {
    const tags = tagsInput.value
      ? JSON.stringify(tagsInput.value.split(',').map((t) => t.trim()).filter((t) => t))
      : undefined;

    const createdPost = await userApi.publishPost({
      title: newPost.value.title,
      content: newPost.value.content,
      category: newPost.value.category || 'GENERAL',
      postType: newPost.value.postType || 'TOPIC_DISCUSSION',
      analysisTarget: newPost.value.postType === 'DATA_ANALYSIS' ? (newPost.value.analysisTarget || undefined) : undefined,
      tags,
      status: 1,
      allowComment: 1,
    } as ForumPost);

    if (createdPost.id && newPost.value.postType === 'DATA_ANALYSIS' && selectedFiles.value.length > 0) {
      for (const file of selectedFiles.value) {
        const upload = await userApi.uploadForumAttachmentFile(file);
        await userApi.addForumAttachment({
          postId: createdPost.id,
          fileName: upload.fileName,
          fileUrl: upload.fileUrl,
          fileSize: upload.fileSize,
          fileType: inferAttachmentType(upload.fileName),
        });
      }
    }

    message.success('发布成功');
    showPublishDialog.value = false;
    newPost.value = { title: '', content: '', category: 'GENERAL', postType: 'TOPIC_DISCUSSION', analysisTarget: '' };
    tagsInput.value = '';
    selectedFiles.value = [];
    loadPosts();
  } catch (error) {
    console.error('发布失败:', error);
    message.error('发布失败');
  }
};

const inferAttachmentType = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'IMAGE';
  if (['csv', 'json', 'txt', 'xlsx', 'xls'].includes(ext)) return 'DATASET';
  if (['pdf', 'doc', 'docx', 'ppt', 'pptx'].includes(ext)) return 'REPORT';
  return 'FILE';
};

onMounted(() => {
  loadTopPosts();
  loadPosts();
});
</script>

<style scoped lang="less">
.forum-list {
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

.filter-select {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
}

.top-posts {
  margin-bottom: 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 3px solid rgba(102, 126, 234, 0.6);
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
  cursor: pointer;

  &:hover {
    transform: translateX(4px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.4);
  }

  &.top-post {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    border-color: rgba(102, 126, 234, 0.4);
  }
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
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

.type-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.type-topic {
  background: linear-gradient(135deg, #5f72bd, #9b23ea);
}

.type-analysis {
  background: linear-gradient(135deg, #00b894, #00cec9);
}

.reliability-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.reliability-none {
  background: linear-gradient(135deg, #636e72, #2d3436);
}

.reliability-progress {
  background: linear-gradient(135deg, #fdcb6e, #e17055);
}

.reliability-failed {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
}

.reliability-certified {
  background: linear-gradient(135deg, #00b894, #0984e3);
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

.text-center {
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
}

// 对话框样式
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.dialog-content {
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.95), rgba(22, 33, 62, 0.95));
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
  }
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
  }
}

.dialog-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;

  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
  }

  input,
  select,
  textarea {
    width: 100%;
    padding: 10px 14px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(102, 126, 234, 0.3);
    border-radius: 8px;
    color: #fff;
    font-size: 14px;
    font-family: inherit;

    &::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }

    &:focus {
      outline: none;
      border-color: rgba(102, 126, 234, 0.6);
      background: rgba(255, 255, 255, 0.08);
    }
  }

  textarea {
    resize: vertical;
    min-height: 120px;
  }
}

.file-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.file-remove {
  border: none;
  background: rgba(255, 107, 107, 0.2);
  color: #ff8a8a;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid rgba(102, 126, 234, 0.2);
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

.search-input,
.filter-select {
  background: #ffffff;
  border-color: #dbe8f4;
  color: #0f172a;
}

.search-input::placeholder {
  color: #94a3b8;
}

.filter-select {
  color-scheme: light;
}

.section-title {
  color: #0f172a;
  border-left-color: #0284c7;
}

.post-card {
  background: #ffffff;
  border: 1px solid #dbe8f4;
}

.post-card:hover {
  box-shadow: 0 10px 24px -12px rgba(2, 132, 199, 0.35);
}

.post-card.top-post {
  background: linear-gradient(135deg, #f0f9ff, #f8fbff);
  border-color: #bae6fd;
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

.text-center {
  color: #334155;
}

.dialog-content {
  background: #ffffff;
  border: 1px solid #dbe8f4;
}

.dialog-header {
  border-bottom-color: #dbe8f4;

  h3 {
    color: #0f172a;
  }
}

.close-btn {
  color: #334155;
  &:hover {
    background: #f1f5f9;
    color: #0f172a;
  }
}

.form-group {
  label {
    color: #334155;
  }

  input,
  select,
  textarea {
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
}

.file-item {
  border-color: #dbe8f4;
  color: #1e293b;
}

.forum-list .post-meta .author,
.forum-list .post-meta .time,
.forum-list .post-meta .views,
.forum-list .post-meta .likes,
.forum-list .post-meta .comments,
.forum-list .post-content {
  color: #334155 !important;
}

.dialog-footer {
  border-top-color: #dbe8f4;
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

  .dialog-content {
    width: 95%;
    margin: 20px;
  }
}
</style>

