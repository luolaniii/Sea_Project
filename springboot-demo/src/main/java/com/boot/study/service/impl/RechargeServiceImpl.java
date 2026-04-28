package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.dao.MembershipPlanMapper;
import com.boot.study.dao.RechargeOrderMapper;
import com.boot.study.dao.UserMembershipMapper;
import com.boot.study.entity.MembershipPlan;
import com.boot.study.entity.RechargeOrder;
import com.boot.study.entity.UserMembership;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.RechargeService;
import com.boot.study.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 充值会员 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeServiceImpl implements RechargeService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private final MembershipPlanMapper planMapper;
    private final RechargeOrderMapper orderMapper;
    private final UserMembershipMapper membershipMapper;
    private final WalletService walletService;

    @Override
    public List<MembershipPlan> listActivePlans() {
        return planMapper.selectList(new LambdaQueryWrapper<MembershipPlan>()
                .eq(MembershipPlan::getStatus, 1)
                .orderByAsc(MembershipPlan::getSortOrder)
                .orderByAsc(MembershipPlan::getPriceYuan));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createOrder(Long userId, Long planId) {
        if (userId == null || planId == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "参数缺失");
        }
        MembershipPlan plan = planMapper.selectById(planId);
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "套餐不存在或已下架");
        }
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setAmountYuan(plan.getPriceYuan());
        order.setStatus(STATUS_PENDING);
        orderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder mockPay(Long userId, Long orderId, String mockMethod) {
        RechargeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException(403, "无权操作他人订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "订单状态不允许支付");
        }

        MembershipPlan plan = planMapper.selectById(order.getPlanId());
        if (plan == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "套餐已不存在");
        }

        order.setStatus(STATUS_PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setMockPayMethod(mockMethod == null ? "WECHAT_MOCK" : mockMethod);
        orderMapper.updateById(order);

        // 续会员
        extendMembership(userId, plan, order.getId());

        // 发 bonus 币
        if (plan.getBonusCoins() != null && plan.getBonusCoins() > 0) {
            walletService.grant(userId, plan.getBonusCoins(), "RECHARGE_BONUS",
                    "ORDER", order.getId(),
                    "充值 " + plan.getName() + " 赠送" );
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder cancelOrder(Long userId, Long orderId) {
        RechargeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException(403, "无权操作他人订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "订单状态不允许取消");
        }
        order.setStatus(STATUS_CANCELLED);
        orderMapper.updateById(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder adminRefund(Long orderId, String remark, Long operatorId) {
        RechargeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "订单不存在");
        }
        if (!STATUS_PAID.equals(order.getStatus())) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "仅已支付订单可退款");
        }
        order.setStatus(STATUS_REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        order.setRefundRemark(remark);
        orderMapper.updateById(order);

        MembershipPlan plan = planMapper.selectById(order.getPlanId());
        if (plan != null) {
            // 扣回 bonus_coins（钱包余额不足时仍记账，转为负余额由后续业务约束处理 — 但这里使用 grant 负值)
            int bonus = plan.getBonusCoins() == null ? 0 : plan.getBonusCoins();
            if (bonus > 0) {
                walletService.grant(order.getUserId(), -bonus, "REFUND",
                        "ORDER", order.getId(),
                        "订单 " + order.getOrderNo() + " 退款，回收赠币");
            }
            // 会员到期日回退（按本套餐天数减回）
            UserMembership ms = getMembership(order.getUserId());
            if (ms != null && ms.getExpiresAt() != null) {
                LocalDateTime newExpire = ms.getExpiresAt().minusDays(plan.getDays() == null ? 0 : plan.getDays());
                LocalDateTime now = LocalDateTime.now();
                if (newExpire.isBefore(now)) {
                    newExpire = now.minusSeconds(1);
                }
                ms.setExpiresAt(newExpire);
                membershipMapper.updateById(ms);
            }
        }
        log.info("订单 {} 已被管理员 {} 退款", order.getOrderNo(), operatorId);
        return order;
    }

    @Override
    public IPage<RechargeOrder> myOrderPage(Long userId, int pageNum, int pageSize, String status) {
        int p = pageNum < 1 ? 1 : pageNum;
        int s = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<RechargeOrder> q = new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getUserId, userId)
                .orderByDesc(RechargeOrder::getCreatedTime);
        if (status != null && !status.isEmpty()) {
            q.eq(RechargeOrder::getStatus, status);
        }
        return orderMapper.selectPage(new Page<>(p, s), q);
    }

    @Override
    public IPage<RechargeOrder> adminOrderPage(int pageNum, int pageSize, String status, String orderNo) {
        int p = pageNum < 1 ? 1 : pageNum;
        int s = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<RechargeOrder> q = new LambdaQueryWrapper<RechargeOrder>()
                .orderByDesc(RechargeOrder::getCreatedTime);
        if (status != null && !status.isEmpty()) {
            q.eq(RechargeOrder::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            q.like(RechargeOrder::getOrderNo, orderNo.trim());
        }
        return orderMapper.selectPage(new Page<>(p, s), q);
    }

    @Override
    public UserMembership getMembership(Long userId) {
        return membershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId));
    }

    private void extendMembership(Long userId, MembershipPlan plan, Long orderId) {
        UserMembership existing = getMembership(userId);
        LocalDateTime now = LocalDateTime.now();
        int days = plan.getDays() == null ? 0 : plan.getDays();
        if (existing == null) {
            UserMembership ms = new UserMembership();
            ms.setUserId(userId);
            ms.setPlanId(plan.getId());
            ms.setStartedAt(now);
            ms.setExpiresAt(now.plusDays(days));
            ms.setLastOrderId(orderId);
            membershipMapper.insert(ms);
        } else {
            LocalDateTime base = (existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now))
                    ? existing.getExpiresAt() : now;
            existing.setPlanId(plan.getId());
            if (existing.getStartedAt() == null || existing.getExpiresAt() == null
                    || existing.getExpiresAt().isBefore(now)) {
                existing.setStartedAt(now);
            }
            existing.setExpiresAt(base.plusDays(days));
            existing.setLastOrderId(orderId);
            membershipMapper.updateById(existing);
        }
    }

    private String genOrderNo() {
        // 形如 R20260428-9F3A2C8B
        String date = java.time.LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "R" + date + "-" + suffix;
    }
}
