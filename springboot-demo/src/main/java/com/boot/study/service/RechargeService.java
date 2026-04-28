package com.boot.study.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.study.entity.MembershipPlan;
import com.boot.study.entity.RechargeOrder;
import com.boot.study.entity.UserMembership;

import java.util.List;

/**
 * 充值会员业务 Service
 */
public interface RechargeService {

    /** 获取上架的会员套餐 */
    List<MembershipPlan> listActivePlans();

    /** 创建订单（PENDING 状态） */
    RechargeOrder createOrder(Long userId, Long planId);

    /** 模拟支付：把订单标记为 PAID，发币 + 续会员 */
    RechargeOrder mockPay(Long userId, Long orderId, String mockMethod);

    /** 用户取消订单（仅 PENDING 可取消） */
    RechargeOrder cancelOrder(Long userId, Long orderId);

    /** 管理员退款（PAID → REFUNDED）：扣回 bonus_coins，会员到期日回退 */
    RechargeOrder adminRefund(Long orderId, String remark, Long operatorId);

    /** 我的订单分页 */
    IPage<RechargeOrder> myOrderPage(Long userId, int pageNum, int pageSize, String status);

    /** 管理员订单分页 */
    IPage<RechargeOrder> adminOrderPage(int pageNum, int pageSize, String status, String orderNo);

    /** 我的会员状态（不存在则返回 null） */
    UserMembership getMembership(Long userId);
}
