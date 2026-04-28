package com.boot.study.controller.user;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.UserWallet;
import com.boot.study.entity.WalletTransaction;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 钱包
 */
@RestController
@RequestMapping("/api/user/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public Result<UserWallet> myWallet() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        return Result.success(walletService.getOrCreate(userId));
    }

    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> transactions(@RequestParam(defaultValue = "50") Integer limit) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        return Result.success(walletService.recentTransactions(userId, limit == null ? 50 : limit));
    }
}
