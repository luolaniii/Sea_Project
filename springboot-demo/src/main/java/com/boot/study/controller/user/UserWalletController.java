package com.boot.study.controller.user;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.UserWallet;
import com.boot.study.entity.WalletTransaction;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 用户端 - 钱包
 *
 * <p>所有 GET 接口在数据库表尚未初始化（如未执行 expert_wallet_recharge_*.sql）时，
 * 都会返回安全的空默认值，避免在用户中心页面持续弹出"系统异常"。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/user/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public Result<UserWallet> myWallet() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        try {
            return Result.success(walletService.getOrCreate(userId));
        } catch (Exception e) {
            log.warn("钱包查询失败（数据库表可能尚未迁移）: {}", e.getMessage());
            return Result.success(emptyWallet(userId));
        }
    }

    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> transactions(@RequestParam(defaultValue = "50") Integer limit) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        try {
            return Result.success(walletService.recentTransactions(userId, limit == null ? 50 : limit));
        } catch (Exception e) {
            log.warn("钱包流水查询失败: {}", e.getMessage());
            return Result.success(Collections.emptyList());
        }
    }

    private UserWallet emptyWallet(Long userId) {
        UserWallet w = new UserWallet();
        w.setUserId(userId);
        w.setBalanceCoins(0);
        w.setTotalEarnedCoins(0);
        w.setTotalSpentCoins(0);
        return w;
    }
}
