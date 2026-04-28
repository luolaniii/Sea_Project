package com.boot.study.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.MembershipPlan;
import com.boot.study.entity.RechargeOrder;
import com.boot.study.entity.UserMembership;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.RechargeService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 充值/会员
 */
@Slf4j
@RestController
@RequestMapping("/api/user/recharge")
@RequiredArgsConstructor
public class UserRechargeController {

    private final RechargeService rechargeService;

    /** 套餐列表 */
    @GetMapping("/plans")
    public Result<List<MembershipPlan>> plans() {
        try {
            return Result.success(rechargeService.listActivePlans());
        } catch (Exception e) {
            log.warn("套餐查询失败（数据库表可能尚未迁移）: {}", e.getMessage());
            return Result.success(java.util.Collections.emptyList());
        }
    }

    /** 创建订单 */
    @PostMapping("/order")
    public Result<RechargeOrder> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        Object planIdObj = body == null ? null : body.get("planId");
        if (planIdObj == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "planId 不能为空");
        }
        Long planId = PathLongParser.tryParse(String.valueOf(planIdObj));
        if (planId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "planId 无效");
        }
        try {
            return Result.success(rechargeService.createOrder(userId, planId));
        } catch (BadSqlGrammarException sql) {
            return Result.fail(500, "数据库尚未初始化，请先执行 expert_wallet_recharge_*.sql");
        }
    }

    /** 模拟付款 */
    @PostMapping("/order/{id}/mock-pay")
    public Result<RechargeOrder> mockPay(@PathVariable("id") String idStr,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "订单ID无效");
        String method = body == null ? null : (String) body.get("payMethod");
        try {
            return Result.success(rechargeService.mockPay(userId, id, method));
        } catch (BadSqlGrammarException sql) {
            return Result.fail(500, "数据库尚未初始化，请先执行 expert_wallet_recharge_*.sql");
        }
    }

    /** 取消订单 */
    @PostMapping("/order/{id}/cancel")
    public Result<RechargeOrder> cancel(@PathVariable("id") String idStr) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "订单ID无效");
        try {
            return Result.success(rechargeService.cancelOrder(userId, id));
        } catch (BadSqlGrammarException sql) {
            return Result.fail(500, "数据库尚未初始化，请先执行 expert_wallet_recharge_*.sql");
        }
    }

    /** 我的订单分页 */
    @GetMapping("/orders/page")
    public Result<Map<String, Object>> myOrders(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String status) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        Map<String, Object> data = new HashMap<>();
        try {
            IPage<RechargeOrder> page = rechargeService.myOrderPage(userId, pageNum, pageSize, status);
            data.put("list", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
        } catch (Exception e) {
            log.warn("订单分页查询失败（数据库表可能尚未迁移）: {}", e.getMessage());
            data.put("list", java.util.Collections.emptyList());
            data.put("total", 0);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
        }
        return Result.success(data);
    }

    /** 我的会员状态 */
    @GetMapping("/membership/me")
    public Result<UserMembership> myMembership() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        try {
            return Result.success(rechargeService.getMembership(userId));
        } catch (Exception e) {
            log.warn("会员状态查询失败（数据库表可能尚未迁移）: {}", e.getMessage());
            return Result.success((UserMembership) null);
        }
    }
}
