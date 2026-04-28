/**
 * 专家 / 钱包 / 充值会员 相关 API
 */

import http from './http';
import { toPathId } from './path-id';
import type { PageReq, PageBean } from './api-user';

// ==================== 类型 ====================

export interface ExpertApplication {
  id?: string | number;
  userId?: string | number;
  realName?: string;
  organization?: string;
  profession?: string;
  expertiseTags?: string;
  applicationReason?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewerId?: string | number;
  reviewRemark?: string;
  reviewedAt?: string;
  createdTime?: string;
}

export interface UserWallet {
  id?: string | number;
  userId?: string | number;
  balanceCoins: number;
  totalEarnedCoins: number;
  totalSpentCoins: number;
}

export interface WalletTransaction {
  id?: string | number;
  userId?: string | number;
  type: 'EARN_BADGE' | 'EARN_REVIEW' | 'RECHARGE_BONUS' | 'SPEND' | 'REFUND' | string;
  amount: number;
  balanceAfter: number;
  refType?: string;
  refId?: string | number;
  remark?: string;
  createdTime?: string;
}

export interface Badge {
  id?: string | number;
  code: string;
  name: string;
  description?: string;
  iconEmoji?: string;
  thresholdCount: number;
  rewardCoins: number;
  sortOrder?: number;
  status?: number;
}

export interface UserBadge {
  id?: string | number;
  userId?: string | number;
  badgeId: string | number;
  evaluatedCountSnapshot?: number;
  awardedAt?: string;
}

export interface MembershipPlan {
  id: string | number;
  planCode: string;
  name: string;
  description?: string;
  days: number;
  priceYuan: number;
  bonusCoins: number;
  sortOrder?: number;
  status?: number;
}

export interface RechargeOrder {
  id?: string | number;
  orderNo: string;
  userId?: string | number;
  planId: string | number;
  amountYuan: number;
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED' | string;
  mockPayMethod?: string;
  paidAt?: string;
  refundedAt?: string;
  refundRemark?: string;
  createdTime?: string;
}

export interface UserMembership {
  id?: string | number;
  userId?: string | number;
  planId?: string | number;
  startedAt?: string;
  expiresAt?: string;
  lastOrderId?: string | number;
}

// ==================== API ====================

export const expertApi = {
  submitApplication: (form: ExpertApplication): Promise<ExpertApplication> => {
    return http.post('/user/expert-application', form);
  },
  getMyApplication: (silent = false): Promise<ExpertApplication | null> => {
    return http.get('/user/expert-application/me', silent ? ({ silentError: true } as any) : undefined);
  },
};

export const walletApi = {
  getMyWallet: (silent = false): Promise<UserWallet> => {
    return http.get('/user/wallet/me', silent ? ({ silentError: true } as any) : undefined);
  },
  getTransactions: (limit = 50, silent = false): Promise<WalletTransaction[]> => {
    return http.get('/user/wallet/transactions', { params: { limit }, ...(silent ? { silentError: true } : {}) } as any);
  },
  getMyBadges: (silent = false): Promise<UserBadge[]> => {
    return http.get('/user/badge/me', silent ? ({ silentError: true } as any) : undefined);
  },
  getBadgeCatalog: (silent = false): Promise<Badge[]> => {
    return http.get('/user/badge/catalog', silent ? ({ silentError: true } as any) : undefined);
  },
};

export const rechargeApi = {
  listPlans: (silent = false): Promise<MembershipPlan[]> => {
    return http.get('/user/recharge/plans', silent ? ({ silentError: true } as any) : undefined);
  },
  createOrder: (planId: string | number): Promise<RechargeOrder> => {
    return http.post('/user/recharge/order', { planId: String(planId) });
  },
  mockPay: (orderId: string | number, payMethod: 'WECHAT_MOCK' | 'ALIPAY_MOCK' = 'WECHAT_MOCK'): Promise<RechargeOrder> => {
    return http.post(`/user/recharge/order/${toPathId(orderId)}/mock-pay`, { payMethod });
  },
  cancelOrder: (orderId: string | number): Promise<RechargeOrder> => {
    return http.post(`/user/recharge/order/${toPathId(orderId)}/cancel`);
  },
  getMyOrders: (req: PageReq, silent = false): Promise<PageBean<RechargeOrder>> => {
    return http.get('/user/recharge/orders/page', { params: req, ...(silent ? { silentError: true } : {}) } as any);
  },
  getMyMembership: (silent = false): Promise<UserMembership | null> => {
    return http.get('/user/recharge/membership/me', silent ? ({ silentError: true } as any) : undefined);
  },
};

export const adminExpertApi = {
  page: (req: PageReq & { status?: string; keyword?: string }, silent = false): Promise<PageBean<ExpertApplication>> => {
    return http.get('/admin/expert-application/page', { params: req, ...(silent ? { silentError: true } : {}) } as any);
  },
  review: (id: string | number, approve: boolean, remark = ''): Promise<ExpertApplication> => {
    return http.post(`/admin/expert-application/${toPathId(id)}/review`, { approve, remark });
  },
};

export const adminRechargeApi = {
  page: (req: PageReq & { status?: string; orderNo?: string }, silent = false): Promise<PageBean<RechargeOrder>> => {
    return http.get('/admin/recharge/page', { params: req, ...(silent ? { silentError: true } : {}) } as any);
  },
  refund: (id: string | number, remark = ''): Promise<RechargeOrder> => {
    return http.post(`/admin/recharge/${toPathId(id)}/refund`, { remark });
  },
};
