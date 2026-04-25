package com.boot.study.keygen;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

@Component
public class CustomIdGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) {
        //全局唯一ID
        return SnowflakeKeygen.getNextId();
    }
}
