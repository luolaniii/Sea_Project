package com.boot.study.controller.common;

import com.boot.study.bean.Result;
import com.boot.study.response.EnumItem;
import com.boot.study.service.EnumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 枚举数据接口控制器
 * <p>
 * 提供枚举数据查询接口，包括：
 * - 获取所有枚举数据
 * - 根据枚举名称获取枚举数据
 * <p>
 * Controller层只负责参数接收和结果返回，业务逻辑在Service层实现
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/common/enum")
@RequiredArgsConstructor
public class EnumController {

    /**
     * 枚举数据服务
     */
    private final EnumService enumService;

    /**
     * 获取所有枚举数据
     *
     * @return 所有枚举数据Map
     */
    @GetMapping("/all")
    public Result<Map<String, List<EnumItem>>> getAllEnums() {
        Map<String, List<EnumItem>> result = enumService.getAllEnums();
        return Result.success(result);
    }

    /**
     * 根据枚举名称获取枚举数据
     *
     * @param enumName 枚举类型名称
     * @return 枚举选项列表
     */
    @GetMapping("/get")
    public Result<List<EnumItem>> getEnum(@RequestParam String enumName) {
        List<EnumItem> result = enumService.getEnumByName(enumName);
        return Result.success(result);
    }
}

