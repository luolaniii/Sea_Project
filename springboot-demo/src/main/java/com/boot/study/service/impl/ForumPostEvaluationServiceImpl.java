package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ForumPostEvaluationMapper;
import com.boot.study.entity.ForumPost;
import com.boot.study.entity.ForumPostEvaluation;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ForumPostEvaluationService;
import com.boot.study.service.ForumPostService;
import com.boot.study.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 论坛帖子评估 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ForumPostEvaluationServiceImpl extends BaseServiceImpl<ForumPostEvaluationMapper, ForumPostEvaluation>
        implements ForumPostEvaluationService {

    private static final int REQUIRED_EXPERT_EVALUATIONS = 3;
    private static final BigDecimal RELIABILITY_PASS_SCORE = new BigDecimal("70");

    private final ForumPostService forumPostService;
    private final WalletService walletService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostEvaluation submitEvaluation(ForumPostEvaluation evaluation) {
        if (LoginInfo.getLoginInfo() == null || LoginInfo.getUserId() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        String role = LoginInfo.getUserRole();
        if (!"expert".equals(role) && !"admin".equals(role)) {
            throw new ServiceException(403, "仅专家或管理员可提交评估");
        }
        if (evaluation.getPostId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "postId不能为空");
        }
        ForumPost post = forumPostService.getById(evaluation.getPostId());
        if (post == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "帖子不存在");
        }

        evaluation.setEvaluatorId(LoginInfo.getUserId());
        if (LoginInfo.getLoginInfo().getUsername() != null) {
            evaluation.setEvaluatorName(LoginInfo.getLoginInfo().getUsername());
        }
        evaluation.setIsExpert(1);
        if (evaluation.getStatus() == null) {
            evaluation.setStatus(1);
        }
        BigDecimal accuracy = defaultScore(evaluation.getScoreAccuracy());
        BigDecimal risk = defaultScore(evaluation.getScoreRisk());
        BigDecimal reasoning = defaultScore(evaluation.getScoreReasoning());
        evaluation.setScoreAccuracy(accuracy);
        evaluation.setScoreRisk(risk);
        evaluation.setScoreReasoning(reasoning);
        evaluation.setTotalScore(accuracy.add(risk).add(reasoning)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP));

        ForumPostEvaluation existing = getOne(new LambdaQueryWrapper<ForumPostEvaluation>()
                .eq(ForumPostEvaluation::getPostId, evaluation.getPostId())
                .eq(ForumPostEvaluation::getEvaluatorId, evaluation.getEvaluatorId())
                .last("limit 1"));
        if (existing == null) {
            save(evaluation);
        } else {
            evaluation.setId(existing.getId());
            updateById(evaluation);
        }

        refreshPostReliability(evaluation.getPostId());

        // 评审奖励 hook：按当前评估人累计有效评估数发放徽章 + 奖励币
        // 失败不应阻断评估保存（钱包表未迁移、奖励链异常等）
        Long evaluatorId = evaluation.getEvaluatorId();
        if (evaluatorId != null) {
            try {
                long count = count(new LambdaQueryWrapper<ForumPostEvaluation>()
                        .eq(ForumPostEvaluation::getEvaluatorId, evaluatorId)
                        .eq(ForumPostEvaluation::getStatus, 1));
                walletService.awardForEvaluation(evaluatorId, count);
            } catch (Exception ex) {
                // walletService.awardForEvaluation 自身已使用 REQUIRES_NEW + try/catch，这里再保险一次
                org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn("奖励 hook 调用异常，已忽略: {}", ex.getMessage());
            }
        }
        return evaluation;
    }

    @Override
    public List<ForumPostEvaluation> listByPostId(Long postId) {
        return list(new LambdaQueryWrapper<ForumPostEvaluation>()
                .eq(ForumPostEvaluation::getPostId, postId)
                .eq(ForumPostEvaluation::getStatus, 1)
                .orderByDesc(ForumPostEvaluation::getTotalScore)
                .orderByDesc(ForumPostEvaluation::getCreatedTime));
    }

    private BigDecimal defaultScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "评分必须在0-100范围");
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private void refreshPostReliability(Long postId) {
        List<ForumPostEvaluation> evaluations = list(new LambdaQueryWrapper<ForumPostEvaluation>()
                .eq(ForumPostEvaluation::getPostId, postId)
                .eq(ForumPostEvaluation::getStatus, 1));
        ForumPost post = forumPostService.getById(postId);
        if (post == null) {
            return;
        }
        int count = evaluations.size();
        post.setEvaluationCount(count);
        if (count == 0) {
            post.setReliabilityScore(BigDecimal.ZERO);
            post.setReliabilityStatus(0);
        } else {
            BigDecimal sum = evaluations.stream()
                    .map(ForumPostEvaluation::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
            post.setReliabilityScore(avg);
            // 评估结束标准：至少3名专家提交有效评估；结束后均分>=70才判定为可信/已认证。
            post.setReliabilityStatus(count >= REQUIRED_EXPERT_EVALUATIONS && avg.compareTo(RELIABILITY_PASS_SCORE) >= 0 ? 2 : 1);
        }
        forumPostService.updateById(post);
    }
}
