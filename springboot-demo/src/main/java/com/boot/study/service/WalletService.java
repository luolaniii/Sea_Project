package com.boot.study.service;

import com.boot.study.entity.UserWallet;
import com.boot.study.entity.WalletTransaction;

import java.util.List;

/**
 * 钱包业务 Service
 */
public interface WalletService {

    /** 获取或创建用户钱包 */
    UserWallet getOrCreate(Long userId);

    /** 增加币（写流水） */
    UserWallet grant(Long userId, int coins, String type, String refType, Long refId, String remark);

    /** 扣减币（不足返回 false） */
    boolean spend(Long userId, int coins, String refType, Long refId, String remark);

    /**
     * 评估完成后：根据 evaluatedCount 检查解锁徽章。
     * 命中阈值 → 发徽章 + 发奖励币
     */
    void awardForEvaluation(Long userId, long evaluatedCount);

    /** 列出用户最近 N 条流水 */
    List<WalletTransaction> recentTransactions(Long userId, int limit);
}
