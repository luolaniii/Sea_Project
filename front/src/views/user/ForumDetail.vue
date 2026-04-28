<template>
  <div class="forum-detail">
    <div v-if="loading" class="text-center">加载中...</div>
    <div v-else-if="!post" class="text-center">帖子不存在</div>
    <div v-else class="detail-content">
      <!-- 返回按钮 -->
      <button class="back-btn" @click="goBack">
        <span>←</span> 返回列表
      </button>

      <!-- 帖子内容 -->
      <div class="post-content card">
        <div class="post-header">
          <div class="post-badges">
            <span v-if="post.isTop" class="top-badge">置顶</span>
            <span v-if="post.isEssence" class="essence-badge">精华</span>
            <span class="category-badge">{{ getCategoryLabel(post.category) }}</span>
            <span :class="['type-badge', post.postType === 'DATA_ANALYSIS' ? 'type-analysis' : 'type-topic']">
              {{ post.postType === 'DATA_ANALYSIS' ? '数据分析' : '主题讨论' }}
            </span>
            <span v-if="post.postType === 'DATA_ANALYSIS'" :class="['reliability-badge', getReliabilityClass(post)]">
              {{ getReliabilityText(post) }}（{{ post.reliabilityScore || 0 }}）
            </span>
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <span class="author"><Icon name="user" :size="13" /> {{ post.authorName }}</span>
            <span class="time">{{ formatTime(post.createdTime) }}</span>
            <span class="views"><Icon name="eye" :size="13" /> {{ post.viewCount || 0 }} 浏览</span>
            <span class="likes" :class="{ liked: isLiked }" @click="handleLike">
              <Icon name="heart" :size="13" /> {{ post.likeCount || 0 }}
            </span>
            <span class="comments"><Icon name="chat" :size="13" /> {{ post.commentCount || 0 }} 评论</span>
          </div>
        </div>
        <div class="post-body">
          <div v-if="post.postType === 'DATA_ANALYSIS' && post.analysisTarget" class="analysis-target">
            分析目标：{{ post.analysisTarget }}
          </div>
          <div class="post-tags" v-if="parseTags(post.tags).length > 0">
            <span
              v-for="tag in parseTags(post.tags)"
              :key="tag"
              class="tag"
            >
              {{ tag }}
            </span>
          </div>
          <div class="post-text" v-html="formatContent(post.content)"></div>
        </div>
        <div v-if="post.postType === 'DATA_ANALYSIS'" class="analysis-sections">
          <div class="analysis-card">
            <div class="analysis-title">评选投票</div>
            <div class="vote-summary">
              <span>支持：{{ voteSummary.supportCount }}</span>
              <span>反对：{{ voteSummary.opposeCount }}</span>
              <span>总票数：{{ voteSummary.totalCount }}</span>
            </div>
            <div class="vote-actions">
              <button class="btn btn-primary" @click="handleVote(1)">支持分析</button>
              <button class="btn btn-danger" @click="handleVote(-1)">质疑分析</button>
            </div>
          </div>
          <div class="analysis-card">
            <div class="analysis-title">专家评估</div>
            <div class="review-standard">
              <span>结束标准：至少 3 名专家提交有效评估。</span>
              <span>可信标准：专家综合均分达到 70 分及以上。</span>
              <b>{{ evaluationProgressText }}</b>
            </div>
            <div v-if="evaluations.length === 0" class="empty-hint">暂无专家评估</div>
            <div v-else class="evaluation-list">
              <div v-for="item in evaluations" :key="item.id" class="evaluation-item">
                <div class="evaluation-head">
                  <span>{{ item.evaluatorName || '专家' }}</span>
                  <span>综合分：{{ item.totalScore || 0 }}</span>
                </div>
                <div class="evaluation-sub">
                  准确性 {{ item.scoreAccuracy || 0 }} / 风险 {{ item.scoreRisk || 0 }} / 可解释性 {{ item.scoreReasoning || 0 }}
                </div>
                <div v-if="item.comment" class="evaluation-comment">{{ item.comment }}</div>
              </div>
            </div>
            <div v-if="isExpertUser" class="evaluation-form">
              <textarea v-model="evaluationForm.comment" rows="3" class="comment-input" placeholder="填写专家评估意见"></textarea>
              <div class="score-row">
                <input v-model.number="evaluationForm.scoreAccuracy" type="number" min="0" max="100" class="score-input" placeholder="准确性" />
                <input v-model.number="evaluationForm.scoreRisk" type="number" min="0" max="100" class="score-input" placeholder="风险评估" />
                <input v-model.number="evaluationForm.scoreReasoning" type="number" min="0" max="100" class="score-input" placeholder="可解释性" />
              </div>
              <button class="btn btn-primary" @click="handleSubmitEvaluation">提交评估</button>
            </div>
          </div>
          <div class="analysis-card">
            <div class="analysis-title">成果附件</div>
            <div class="attachment-upload">
              <input type="file" multiple @change="handleDetailFileChange" />
              <button class="btn btn-primary" @click="handleUploadDetailAttachments" :disabled="detailSelectedFiles.length === 0 || detailUploading">
                {{ detailUploading ? '上传中...' : '上传并关联' }}
              </button>
            </div>
            <div v-if="detailSelectedFiles.length > 0" class="attachment-pending">
              待上传：{{ detailSelectedFiles.map((f) => f.name).join('、') }}
            </div>
            <div v-if="attachments.length === 0" class="empty-hint">暂无附件</div>
            <div v-else class="attachment-list">
              <div v-for="item in attachments" :key="item.id" class="attachment-row">
                <a :href="item.fileUrl" target="_blank" class="attachment-link">
                  <span class="attachment-icon"><Icon :name="getAttachmentIcon(item.fileName)" :size="16" color="#00d4ff" /></span>
                  <span>{{ item.fileName }}</span>
                  <span v-if="item.fileSize" class="attachment-size">({{ formatFileSize(item.fileSize) }})</span>
                </a>
                <button v-if="canManageAttachment(item)" class="btn btn-danger btn-xs" @click="handleDeleteAttachment(item.id!)">
                  删除附件
                </button>
              </div>
            </div>
          </div>
          <div class="analysis-card">
            <div class="analysis-title">专家答疑区</div>
            <div v-if="expertAnswers.length === 0" class="empty-hint">暂无专家答疑</div>
            <div v-else class="evaluation-list">
              <div v-for="ans in expertAnswers" :key="ans.id" class="evaluation-item">
                <div class="evaluation-head">
                  <span>{{ ans.expertName || '专家' }}</span>
                  <span v-if="ans.expertTitle">{{ ans.expertTitle }}</span>
                </div>
                <div class="evaluation-comment">{{ ans.answerContent }}</div>
              </div>
            </div>
            <div v-if="isExpertUser" class="evaluation-form">
              <textarea v-model="expertAnswerForm.answerContent" rows="3" class="comment-input" placeholder="填写专家答疑内容"></textarea>
              <button class="btn btn-primary" @click="handleSubmitExpertAnswer">发布答疑</button>
            </div>
          </div>
        </div>
        <div class="post-actions">
          <button
            :class="['action-btn', { active: isLiked }]"
            @click="handleLike"
          >
            <Icon name="heart" :size="14" />
            {{ isLiked ? '已点赞' : '点赞' }}
          </button>
          <button class="action-btn" @click="scrollToComments">
            <Icon name="chat" :size="14" /> 评论
          </button>
        </div>
      </div>

      <!-- 评论区域 -->
      <div class="comments-section card" id="comments">
        <h2 class="section-title">评论 ({{ comments.length }})</h2>

        <!-- 发表评论 -->
        <div class="comment-form">
          <textarea
            v-model="newComment.content"
            rows="4"
            placeholder="写下你的评论..."
            class="comment-input"
          ></textarea>
          <div class="comment-form-actions">
            <button class="btn btn-primary" @click="handleSubmitComment">发表评论</button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-if="comments.length === 0" class="no-comments">
          暂无评论，快来发表第一条评论吧！
        </div>
        <div v-else class="comment-list">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
            :class="{ 'is-reply': comment.parentId && comment.parentId !== 0 }"
          >
            <div class="comment-header">
              <span class="comment-author"><Icon name="user" :size="13" /> {{ comment.userName }}</span>
              <span class="comment-time">{{ formatTime(comment.createdTime) }}</span>
            </div>
            <div class="comment-content">
              <span v-if="comment.replyToUserName" class="reply-to">
                @{{ comment.replyToUserName }}
              </span>
              {{ comment.content }}
            </div>
            <div class="comment-actions">
              <button
                :class="['comment-action', { active: isCommentLiked(comment.id!) }]"
                @click="handleLikeComment(comment.id!)"
              >
                <Icon name="heart" :size="13" />
                {{ comment.likeCount || 0 }}
              </button>
              <button
                class="comment-action"
                @click="handleReply(comment)"
              >
                <Icon name="chat" :size="13" /> 回复
              </button>
              <button
                v-if="isMyComment(comment)"
                class="comment-action delete-action"
                @click="handleDeleteComment(comment.id!)"
              >
                <Icon name="trash" :size="13" /> 删除
              </button>
            </div>

            <!-- 回复表单 -->
            <div v-if="replyingTo != null && String(comment.id) === replyingTo" class="reply-form">
              <textarea
                v-model="replyContent"
                rows="3"
                placeholder="写下你的回复..."
                class="comment-input"
              ></textarea>
              <div class="reply-form-actions">
                <button class="btn btn-default" @click="cancelReply">取消</button>
                <button class="btn btn-primary" @click="handleSubmitReply(comment.id!)">回复</button>
              </div>
            </div>

            <!-- 子评论 -->
            <div v-if="comment.children && comment.children.length > 0" class="comment-children">
              <div
                v-for="child in comment.children"
                :key="child.id"
                class="comment-item is-reply"
              >
                <div class="comment-header">
                  <span class="comment-author"><Icon name="user" :size="13" /> {{ child.userName }}</span>
                  <span class="comment-time">{{ formatTime(child.createdTime) }}</span>
                </div>
                <div class="comment-content">
                  <span v-if="child.replyToUserName" class="reply-to">
                    @{{ child.replyToUserName }}
                  </span>
                  {{ child.content }}
                </div>
                <div class="comment-actions">
                  <button
                    :class="['comment-action', { active: isCommentLiked(child.id!) }]"
                    @click="handleLikeComment(child.id!)"
                  >
                    <Icon name="heart" :size="13" />
                    {{ child.likeCount || 0 }}
                  </button>
                  <button
                    v-if="isMyComment(child)"
                    class="comment-action delete-action"
                    @click="handleDeleteComment(child.id!)"
                  >
                    <Icon name="trash" :size="13" /> 删除
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认 Modal -->
    <Modal
      v-model:visible="deleteModalVisible"
      title="确认删除"
      @confirm="confirmDeleteComment"
    >
      <div style="padding: 20px 0; color: #334155;">
        <p style="font-size: 16px; margin: 0;">确定要删除这条评论吗？删除后无法恢复。</p>
      </div>
    </Modal>

    <Modal
      v-model:visible="deleteAttachmentModalVisible"
      title="确认删除附件"
      @confirm="confirmDeleteAttachment"
    >
      <div style="padding: 20px 0; color: #334155;">
        <p style="font-size: 16px; margin: 0;">确定删除这个附件吗？删除后无法恢复。</p>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { userApi, type ForumPost, type ForumComment, type ForumAttachment, type ForumEvaluation, type ExpertAnswer } from '@/utils/api-user';
import { routeParamId } from '@/utils/path-id';
import { message } from '@/utils/message';
import { useAuthStore } from '@/store/auth';
import Modal from '@/components/Modal.vue';
import Icon from '@/components/Icon.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const post = ref<ForumPost | null>(null);
const comments = ref<ForumComment[]>([]);
const loading = ref(false);
const isLiked = ref(false);
const likedComments = ref<Set<string>>(new Set());
const replyingTo = ref<string | null>(null);
const replyContent = ref('');
const newComment = ref({ content: '' });
const deleteModalVisible = ref(false);
const deletingCommentId = ref<string | null>(null);
const attachments = ref<ForumAttachment[]>([]);
const evaluations = ref<ForumEvaluation[]>([]);
const expertAnswers = ref<ExpertAnswer[]>([]);
const voteSummary = ref({ supportCount: 0, opposeCount: 0, totalCount: 0 });
const evaluationForm = ref({ scoreAccuracy: 80, scoreRisk: 80, scoreReasoning: 80, comment: '' });
const expertAnswerForm = ref({ answerContent: '' });
const detailSelectedFiles = ref<File[]>([]);
const detailUploading = ref(false);
const deleteAttachmentModalVisible = ref(false);
const deletingAttachmentId = ref<string | null>(null);
const isExpertUser = computed(() => authStore.userInfo?.role === 'expert' || authStore.userInfo?.role === 'admin');
const REQUIRED_EVALUATIONS = 3;
const RELIABILITY_PASS_SCORE = 70;
const canManageAttachment = (item: ForumAttachment) => {
  const role = String(authStore.userInfo?.role || '').toLowerCase();
  const currentUserId = authStore.userInfo?.id;
  if (role === 'admin') return true;
  if (currentUserId == null) return false;
  const isPostAuthor = post.value?.authorId != null && String(post.value.authorId) === String(currentUserId);
  const isUploader = item.uploaderId != null && String(item.uploaderId) === String(currentUserId);
  return isPostAuthor || isUploader;
};

const getCategoryLabel = (category?: string) => {
  const map: Record<string, string> = {
    GENERAL: '普通讨论',
    QUESTION: '问题求助',
    SHARE: '经验分享',
    NEWS: '新闻资讯',
  };
  return map[category || ''] || '普通讨论';
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

const formatContent = (content?: string) => {
  if (!content) return '';
  // 简单的Markdown转换（可以后续使用markdown库）
  return content
    .replace(/\n/g, '<br>')
    .replace(/#{3}\s+(.+)/g, '<h3>$1</h3>')
    .replace(/#{2}\s+(.+)/g, '<h2>$1</h2>')
    .replace(/#{1}\s+(.+)/g, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>');
};

const parseTags = (tags?: string): string[] => {
  if (!tags) return [];
  try {
    return JSON.parse(tags);
  } catch {
    return tags.split(',').map((t) => t.trim()).filter((t) => t);
  }
};

const evaluationProgressText = computed(() => {
  const count = Number(post.value?.evaluationCount ?? evaluations.value.length ?? 0);
  const score = Number(post.value?.reliabilityScore ?? 0);
  if (count < REQUIRED_EVALUATIONS) return `当前 ${count}/${REQUIRED_EVALUATIONS}，仍在评估中`;
  if (score >= RELIABILITY_PASS_SCORE) return `已结束：最终均分 ${score}，结论可信`;
  return `已结束：最终均分 ${score}，未达到可信阈值`;
});

const getReliabilityText = (item?: ForumPost | null) => {
  const count = Number(item?.evaluationCount ?? 0);
  const score = Number(item?.reliabilityScore ?? 0);
  if (item?.reliabilityStatus === 2) return '已认证';
  if (count >= REQUIRED_EVALUATIONS) return score >= RELIABILITY_PASS_SCORE ? '已认证' : '未通过认证';
  if (count > 0) return `评估中 ${count}/${REQUIRED_EVALUATIONS}`;
  return '未评选';
};

const getReliabilityClass = (item?: ForumPost | null) => {
  const count = Number(item?.evaluationCount ?? 0);
  const score = Number(item?.reliabilityScore ?? 0);
  if (item?.reliabilityStatus === 2) return 'reliability-certified';
  if (count >= REQUIRED_EVALUATIONS && score < RELIABILITY_PASS_SCORE) return 'reliability-failed';
  if (item?.reliabilityStatus === 1 || count > 0) return 'reliability-progress';
  return 'reliability-none';
};

const loadPost = async () => {
  const id = routeParamId(route.params.id);
  if (!id) return;

  loading.value = true;
  try {
    const data = await userApi.getForumPostById(id);
    post.value = data;
  } catch (error) {
    console.error('加载帖子失败:', error);
    message.error('加载帖子失败');
  } finally {
    loading.value = false;
  }
};

const loadAnalysisData = async () => {
  const id = routeParamId(route.params.id);
  if (!id) return;
  try {
    const [a, v, e, ans] = await Promise.all([
      userApi.getForumAttachments(id),
      userApi.getForumVoteSummary(id),
      userApi.getForumEvaluations(id),
      userApi.getExpertAnswersByPostId(id),
    ]);
    attachments.value = a;
    voteSummary.value = v;
    evaluations.value = e;
    expertAnswers.value = ans;
  } catch (error) {
    console.error('加载分析扩展信息失败:', error);
  }
};

const loadComments = async () => {
  const id = routeParamId(route.params.id);
  if (!id) return;

  try {
    // 后端已经返回树形结构，直接使用
    const data = await userApi.getCommentsByPostId(id);
    comments.value = data;
  } catch (error) {
    console.error('加载评论失败:', error);
    message.error('加载评论失败');
  }
};

const handleLike = async () => {
  if (!post.value?.id) return;

  try {
    if (isLiked.value) {
      await userApi.unlikePost(post.value.id);
      post.value.likeCount = (post.value.likeCount || 0) - 1;
      isLiked.value = false;
      message.success('已取消点赞');
    } else {
      await userApi.likePost(post.value.id);
      post.value.likeCount = (post.value.likeCount || 0) + 1;
      isLiked.value = true;
      message.success('点赞成功');
    }
  } catch (error) {
    console.error('点赞失败:', error);
    message.error('操作失败');
  }
};

const handleVote = async (voteType: 1 | -1) => {
  if (!post.value?.id) return;
  try {
    await userApi.voteForumPost(post.value.id, voteType);
    message.success(voteType === 1 ? '已投支持票' : '已投质疑票');
    const data = await userApi.getForumVoteSummary(post.value.id);
    voteSummary.value = data;
  } catch (error) {
    console.error('投票失败:', error);
    message.error('投票失败');
  }
};

const handleDetailFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;
  detailSelectedFiles.value = Array.from(input.files);
};

const handleUploadDetailAttachments = async () => {
  if (!post.value?.id || detailSelectedFiles.value.length === 0) return;
  try {
    detailUploading.value = true;
    for (const file of detailSelectedFiles.value) {
      const upload = await userApi.uploadForumAttachmentFile(file);
      await userApi.addForumAttachment({
        postId: post.value.id,
        fileName: upload.fileName,
        fileUrl: upload.fileUrl,
        fileSize: upload.fileSize,
        fileType: inferAttachmentType(upload.fileName),
      });
    }
    detailSelectedFiles.value = [];
    attachments.value = await userApi.getForumAttachments(post.value.id);
    message.success('附件上传成功');
  } catch (error) {
    console.error('上传附件失败:', error);
    message.error('上传附件失败');
  } finally {
    detailUploading.value = false;
  }
};

const handleDeleteAttachment = async (attachmentId: string | number) => {
  deletingAttachmentId.value = String(attachmentId);
  deleteAttachmentModalVisible.value = true;
};

const confirmDeleteAttachment = async () => {
  if (!post.value?.id) return;
  if (!deletingAttachmentId.value) return;
  try {
    await userApi.deleteForumAttachment(deletingAttachmentId.value);
    attachments.value = await userApi.getForumAttachments(post.value.id);
    message.success('附件已删除');
    deletingAttachmentId.value = null;
  } catch (error) {
    console.error('删除附件失败:', error);
    message.error('删除附件失败');
  }
};

const inferAttachmentType = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'IMAGE';
  if (['csv', 'json', 'txt', 'xlsx', 'xls'].includes(ext)) return 'DATASET';
  if (['pdf', 'doc', 'docx', 'ppt', 'pptx'].includes(ext)) return 'REPORT';
  return 'FILE';
};

const getAttachmentIcon = (fileName?: string): string => {
  if (!fileName) return 'attach';
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'spark';
  if (['csv', 'json', 'xlsx', 'xls'].includes(ext)) return 'bars';
  if (['txt', 'md'].includes(ext)) return 'edit';
  if (['pdf'].includes(ext)) return 'edit';
  return 'attach';
};

const formatFileSize = (size: number) => {
  if (size < 1024) return `${size}B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`;
  return `${(size / (1024 * 1024)).toFixed(1)}MB`;
};

const handleSubmitEvaluation = async () => {
  if (!post.value?.id) return;
  try {
    await userApi.submitForumEvaluation({
      postId: post.value.id,
      scoreAccuracy: evaluationForm.value.scoreAccuracy,
      scoreRisk: evaluationForm.value.scoreRisk,
      scoreReasoning: evaluationForm.value.scoreReasoning,
      comment: evaluationForm.value.comment || undefined,
    });
    message.success('评估提交成功');
    evaluationForm.value.comment = '';
    evaluations.value = await userApi.getForumEvaluations(post.value.id);
    post.value = await userApi.getForumPostById(post.value.id);
  } catch (error) {
    console.error('提交评估失败:', error);
    message.error('提交评估失败');
  }
};

const handleSubmitExpertAnswer = async () => {
  if (!post.value?.id || !expertAnswerForm.value.answerContent.trim()) {
    message.error('请输入答疑内容');
    return;
  }
  try {
    await userApi.addExpertAnswer({
      postId: post.value.id,
      answerContent: expertAnswerForm.value.answerContent,
      answerType: 'QA',
    });
    message.success('专家答疑已发布');
    expertAnswerForm.value.answerContent = '';
    expertAnswers.value = await userApi.getExpertAnswersByPostId(post.value.id);
  } catch (error) {
    console.error('发布答疑失败:', error);
    message.error('发布答疑失败');
  }
};

const handleSubmitComment = async () => {
  if (!newComment.value.content.trim()) {
    message.error('请输入评论内容');
    return;
  }

  const id = routeParamId(route.params.id);
  if (!id) return;

  try {
    await userApi.addComment({
      postId: id,
      content: newComment.value.content,
      parentId: 0,
    } as ForumComment);

    message.success('评论成功');
    newComment.value.content = '';
    loadComments();
    if (post.value) {
      post.value.commentCount = (post.value.commentCount || 0) + 1;
    }
  } catch (error) {
    console.error('发表评论失败:', error);
    message.error('发表评论失败');
  }
};

const handleReply = (comment: ForumComment) => {
  replyingTo.value = comment.id != null ? String(comment.id) : null;
  replyContent.value = '';
};

const cancelReply = () => {
  replyingTo.value = null;
  replyContent.value = '';
};

const handleSubmitReply = async (parentId: string | number) => {
  if (!replyContent.value.trim()) {
    message.error('请输入回复内容');
    return;
  }

  const id = routeParamId(route.params.id);
  if (!id) return;

  const parentComment = comments.value.find((c) => String(c.id) === String(parentId)) ||
    comments.value.flatMap((c) => c.children || []).find((c) => String(c.id) === String(parentId));

  try {
    await userApi.addComment({
      postId: id,
      content: replyContent.value,
      parentId,
      replyToUserId: parentComment?.userId,
      replyToUserName: parentComment?.userName,
    } as ForumComment);

    message.success('回复成功');
    replyContent.value = '';
    replyingTo.value = null;
    loadComments();
    if (post.value) {
      post.value.commentCount = (post.value.commentCount || 0) + 1;
    }
  } catch (error) {
    console.error('回复失败:', error);
    message.error('回复失败');
  }
};

const commentKey = (commentId: string | number) => String(commentId);

const handleLikeComment = async (commentId: string | number) => {
  const key = commentKey(commentId);
  try {
    if (likedComments.value.has(key)) {
      await userApi.unlikeComment(commentId);
      likedComments.value.delete(key);
      updateCommentLikeCount(commentId, -1);
      message.success('已取消点赞');
    } else {
      await userApi.likeComment(commentId);
      likedComments.value.add(key);
      updateCommentLikeCount(commentId, 1);
      message.success('点赞成功');
    }
  } catch (error) {
    console.error('点赞失败:', error);
    message.error('操作失败');
  }
};

const updateCommentLikeCount = (commentId: string | number, delta: number) => {
  const updateComment = (comment: ForumComment) => {
    if (String(comment.id) === String(commentId)) {
      comment.likeCount = (comment.likeCount || 0) + delta;
      return true;
    }
    if (comment.children) {
      for (const child of comment.children) {
        if (updateComment(child)) return true;
      }
    }
    return false;
  };

  comments.value.forEach(updateComment);
};

const isCommentLiked = (commentId: string | number) => {
  return likedComments.value.has(commentKey(commentId));
};

const isMyComment = (comment: ForumComment) => {
  const uid = authStore.userInfo?.id;
  return uid != null && comment.userId != null && String(uid) === String(comment.userId);
};

const handleDeleteComment = (commentId: string | number) => {
  deletingCommentId.value = String(commentId);
  deleteModalVisible.value = true;
};

const confirmDeleteComment = async () => {
  if (!deletingCommentId.value) return;

  try {
    await userApi.deleteComment(deletingCommentId.value);
    message.success('删除成功');
    // 重新加载评论列表
    loadComments();
    // 更新帖子评论数
    if (post.value) {
      post.value.commentCount = Math.max((post.value.commentCount || 0) - 1, 0);
    }
    deletingCommentId.value = null;
  } catch (error: any) {
    console.error('删除评论失败:', error);
    message.error(error?.response?.data?.message || '删除失败');
  }
};

const scrollToComments = () => {
  document.getElementById('comments')?.scrollIntoView({ behavior: 'smooth' });
};

const goBack = () => {
  router.push('/user/forum');
};

onMounted(() => {
  loadPost();
  loadComments();
  loadAnalysisData();
});
</script>

<style scoped lang="less">
.forum-detail {
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

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 24px;
  transition: all 0.3s;

  &:hover {
    background: rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.5);
    color: #fff;
  }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.post-content {
  padding: 32px;
}

.post-header {
  margin-bottom: 24px;
}

.post-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.top-badge {
  padding: 6px 12px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a6f);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.essence-badge {
  padding: 6px 12px;
  background: linear-gradient(135deg, #feca57, #ff9ff3);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.category-badge {
  padding: 6px 12px;
  background: rgba(102, 126, 234, 0.3);
  border: 1px solid rgba(102, 126, 234, 0.5);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.type-badge,
.reliability-badge {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.95);
}

.type-topic {
  background: rgba(95, 114, 189, 0.5);
}

.type-analysis {
  background: rgba(0, 184, 148, 0.5);
}

.reliability-none {
  background: rgba(99, 110, 114, 0.6);
}

.reliability-progress {
  background: rgba(253, 203, 110, 0.5);
}

.reliability-failed {
  background: rgba(220, 38, 38, 0.62);
}

.reliability-certified {
  background: rgba(0, 184, 148, 0.7);
}

.review-standard {
  display: grid;
  gap: 6px;
  margin: 10px 0 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbe8f4;
  color: #334155;
  font-size: 12px;

  b {
    color: #0369a1;
  }
}

.post-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.post-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  flex-wrap: wrap;
}

.post-meta span {
  cursor: pointer;
  transition: color 0.3s;

  &:hover {
    color: rgba(255, 255, 255, 0.9);
  }

  &.liked {
    color: #ff6b6b;
  }
}

.post-body {
  margin-bottom: 24px;
}

.analysis-target {
  margin-bottom: 12px;
  color: rgba(224, 242, 255, 0.9);
  font-weight: 500;
}

.analysis-sections {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 20px;
}

.analysis-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 10px;
  padding: 14px;
}

.analysis-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #fff;
}

.vote-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: rgba(224, 242, 255, 0.85);
  margin-bottom: 10px;
}

.vote-actions {
  display: flex;
  gap: 10px;
}

.evaluation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evaluation-item {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 8px;
  padding: 10px;
}

.evaluation-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #fff;
}

.evaluation-sub {
  margin-top: 6px;
  color: rgba(224, 242, 255, 0.75);
  font-size: 13px;
}

.evaluation-comment {
  margin-top: 6px;
  color: rgba(224, 242, 255, 0.9);
}

.evaluation-form {
  margin-top: 10px;
}

.score-row {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}

.score-input {
  width: 120px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #fff;
}

.empty-hint {
  color: rgba(224, 242, 255, 0.6);
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 8px;
}

.attachment-upload {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.attachment-pending {
  color: rgba(224, 242, 255, 0.75);
  margin-bottom: 8px;
}

.attachment-link {
  color: #9cd4ff;
  text-decoration: underline;
  display: flex;
  align-items: center;
  gap: 6px;
}

.attachment-icon {
  min-width: 20px;
}

.attachment-size {
  color: rgba(224, 242, 255, 0.65);
  font-size: 12px;
}

.btn-xs {
  padding: 4px 8px;
  font-size: 12px;
}

.post-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.tag {
  padding: 6px 12px;
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.4);
  border-radius: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.post-text {
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    color: #fff;
    margin: 24px 0 16px 0;
  }

  :deep(strong) {
    color: #fff;
    font-weight: 600;
  }

  :deep(code) {
    background: rgba(255, 255, 255, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 14px;
  }
}

.post-actions {
  display: flex;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid rgba(102, 126, 234, 0.2);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;

  &:hover {
    background: rgba(102, 126, 234, 0.2);
    border-color: rgba(102, 126, 234, 0.5);
    color: #fff;
  }

  &.active {
    background: rgba(255, 107, 107, 0.2);
    border-color: rgba(255, 107, 107, 0.5);
    color: #ff6b6b;
  }
}

.comments-section {
  padding: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
}

.comment-form {
  margin-bottom: 32px;
}

.comment-input {
  width: 100%;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 12px;

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }

  &:focus {
    outline: none;
    border-color: rgba(102, 126, 234, 0.6);
    background: rgba(255, 255, 255, 0.08);
  }
}

.comment-form-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(102, 126, 234, 0.3);
  }

  &.is-reply {
    margin-left: 40px;
    background: rgba(255, 255, 255, 0.02);
  }
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-author {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 12px;
  word-break: break-word;
}

.reply-to {
  color: rgba(102, 126, 234, 1);
  font-weight: 500;
  margin-right: 4px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-action {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.9);
  }

  &.active {
    color: #ff6b6b;
  }

  &.delete-action {
    color: rgba(255, 107, 107, 0.8);

    &:hover {
      color: #ff6b6b;
      background: rgba(255, 107, 107, 0.1);
    }
  }
}

.reply-form {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(102, 126, 234, 0.2);
}

.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.comment-children {
  margin-top: 16px;
  padding-left: 20px;
  border-left: 2px solid rgba(102, 126, 234, 0.2);
}

.no-comments {
  text-align: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.text-center {
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
}

/* 浅色主色调可读性覆盖 */
.back-btn {
  background: #ffffff;
  border-color: #dbe8f4;
  color: #334155;

  &:hover {
    background: #f0f9ff;
    border-color: #7dd3fc;
    color: #0f172a;
  }
}

.post-title {
  color: #0f172a;
}

.post-meta {
  color: #64748b;
}

.post-meta span:hover {
  color: #0f172a;
}

.analysis-target {
  color: #334155;
}

.analysis-card {
  background: #f8fbff;
  border-color: #dbe8f4;
}

.analysis-title {
  color: #0f172a;
}

.vote-summary,
.evaluation-sub,
.empty-hint,
.attachment-pending {
  color: #475569;
}

.evaluation-item,
.attachment-row {
  background: #ffffff;
  border-color: #dbe8f4;
}

.evaluation-head,
.evaluation-comment {
  color: #334155;
}

.attachment-link {
  color: #0369a1;
}

.attachment-size {
  color: #64748b;
}

.tag {
  background: #e0f2fe;
  border-color: #bae6fd;
  color: #0369a1;
}

.post-text {
  color: #334155;

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    color: #0f172a;
  }

  :deep(strong) {
    color: #0f172a;
  }

  :deep(code) {
    background: #f1f5f9;
    color: #0f172a;
  }
}

.post-actions {
  border-top-color: #dbe8f4;
}

.action-btn {
  background: #ffffff;
  border-color: #dbe8f4;
  color: #334155;

  &:hover {
    background: #f0f9ff;
    border-color: #7dd3fc;
    color: #0f172a;
  }
}

.section-title {
  color: #0f172a;
  border-bottom-color: #dbe8f4;
}

.comment-input,
.score-input {
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

.comment-item {
  background: #f8fbff;
  border-color: #dbe8f4;

  &:hover {
    background: #f0f9ff;
    border-color: #bae6fd;
  }

  &.is-reply {
    background: #f8fbff;
  }
}

.comment-author {
  color: #0f172a;
}

.comment-time {
  color: #94a3b8;
}

.comment-content {
  color: #334155;
}

.comment-action {
  color: #64748b;

  &:hover {
    background: #e2e8f0;
    color: #334155;
  }
}

.reply-form,
.comment-children {
  border-top-color: #dbe8f4;
  border-left-color: #dbe8f4;
}

.no-comments,
.text-center {
  color: #64748b;
}

// 响应式设计
@media (max-width: 768px) {
  .post-content,
  .comments-section {
    padding: 20px;
  }

  .post-title {
    font-size: 22px;
  }

  .comment-item.is-reply {
    margin-left: 20px;
  }
}
</style>

