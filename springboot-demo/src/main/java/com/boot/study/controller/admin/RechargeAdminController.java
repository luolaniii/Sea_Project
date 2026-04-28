package com.boot.study.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.RechargeOrder;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.RechargeService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端 - 充值订单管理
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/recharge")
@RequiredArgsConstructor
public class RechargeAdminController {

    private final RechargeService rechargeService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String orderNo) {
        IPage<RechargeOrder> p = rechargeService.adminOrderPage(pageNum, pageSize, status, orderNo);
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("pageNum", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /** 退款 */
    @PostMapping("/{id}/refund")
    public Result<RechargeOrder> refund(@PathVariable("id") String idStr,
                                        @RequestBody(required = false) Map<String, Object> body) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "订单ID无效");
        String remark = body == null ? null : (String) body.get("remark");
        return Result.success(rechargeService.adminRefund(id, remark, LoginInfo.getUserId()));
    }
}
