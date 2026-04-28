package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.dao.UserBadgeMapper;
import com.boot.study.dao.UserWalletMapper;
import com.boot.study.dao.WalletTransactionMapper;
import com.boot.study.entity.Badge;
import com.boot.study.entity.UserBadge;
import com.boot.study.entity.UserWallet;
import com.boot.study.entity.WalletTransaction;
import com.boot.study.service.BadgeService;
import com.boot.study.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 钱包 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final BadgeService badgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserWallet getOrCreate(Long userId) {
        UserWallet wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId));
        if (wallet != null) {
            return wallet;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalanceCoins(0);
        wallet.setTotalEarnedCoins(0);
        wallet.setTotalSpentCoins(0);
        userWalletMapper.insert(wallet);
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserWallet grant(Long userId, int coins, String type, String refType, Long refId, String remark) {
        if (coins == 0) {
            return getOrCreate(userId);
        }
        UserWallet wallet = getOrCreate(userId);
        int newBalance = (wallet.getBalanceCoins() == null ? 0 : wallet.getBalanceCoins()) + coins;
        wallet.setBalanceCoins(newBalance);
        if (coins > 0) {
            wallet.setTotalEarnedCoins((wallet.getTotalEarnedCoins() == null ? 0 : wallet.getTotalEarnedCoins()) + coins);
        }
        userWalletMapper.updateById(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(coins);
        tx.setBalanceAfter(newBalance);
        tx.setRefType(refType);
        tx.setRefId(refId);
        tx.setRemark(remark);
        walletTransactionMapper.insert(tx);

        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean spend(Long userId, int coins, String refType, Long refId, String remark) {
        if (coins <= 0) {
            return true;
        }
        UserWallet wallet = getOrCreate(userId);
        int balance = wallet.getBalanceCoins() == null ? 0 : wallet.getBalanceCoins();
        if (balance < coins) {
            return false;
        }
        int newBalance = balance - coins;
        wallet.setBalanceCoins(newBalance);
        wallet.setTotalSpentCoins((wallet.getTotalSpentCoins() == null ? 0 : wallet.getTotalSpentCoins()) + coins);
        userWalletMapper.updateById(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("SPEND");
        tx.setAmount(-coins);
        tx.setBalanceAfter(newBalance);
        tx.setRefType(refType);
        tx.setRefId(refId);
        tx.setRemark(remark);
        walletTransactionMapper.insert(tx);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardForEvaluation(Long userId, long evaluatedCount) {
        try {
            List<Badge> active = badgeService.listActive();
            for (Badge badge : active) {
                Integer threshold = badge.getThresholdCount();
                if (threshold == null || threshold <= 0) continue;
                if (evaluatedCount < threshold) continue;

                // 已得过该徽章则跳过
                Long owned = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId)
                        .eq(UserBadge::getBadgeId, badge.getId()));
                if (owned != null && owned > 0) continue;

                UserBadge ub = new UserBadge();
                ub.setUserId(userId);
                ub.setBadgeId(badge.getId());
                ub.setEvaluatedCountSnapshot((int) evaluatedCount);
                ub.setAwardedAt(LocalDateTime.now());
                userBadgeMapper.insert(ub);

                int reward = badge.getRewardCoins() == null ? 0 : badge.getRewardCoins();
                if (reward > 0) {
                    grant(userId, reward, "EARN_BADGE", "BADGE", badge.getId(),
                            "解锁徽章 " + badge.getName() + " 奖励");
                }
                log.info("用户 {} 解锁徽章 {} (评估数={})", userId, badge.getCode(), evaluatedCount);
            }
        } catch (Exception e) {
            // 奖励链路失败不应阻断评估保存
            log.error("奖励发放异常 userId={} count={}", userId, evaluatedCount, e);
        }
    }

    @Override
    public List<WalletTransaction> recentTransactions(Long userId, int limit) {
        return walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .orderByDesc(WalletTransaction::getCreatedTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }
}
