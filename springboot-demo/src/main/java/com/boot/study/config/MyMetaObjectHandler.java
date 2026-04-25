package com.boot.study.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.boot.study.bean.LoginInfo;
import com.boot.study.entity.BaseDo;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String INSERT_TIME = "createdTime";
    private static final String INSERT_USER = "createdUser";
    private static final String UPDATE_TIME = "updatedTime";
    private static final String UPDATE_USER = "updatedUser";
    private static final String DELETE_FLAG = "deletedFlag";

//    private static final String DELETE_TIME = "deleteTime";
//    private static final String DELETE_USER = "deleteUser";


    @Override
    public void insertFill(MetaObject metaObject) {
        boolean isLogin = !(LoginInfo.getLoginInfo() == null);
        if (!(metaObject.getOriginalObject() instanceof BaseDo)) {
            throw new RuntimeException("类型异常,请检查!");
        }
        if (metaObject.hasGetter(INSERT_USER) && isLogin) {
            this.strictInsertFill(metaObject, INSERT_USER, Long.class, LoginInfo.getUserId());
        }
        if (metaObject.hasGetter(UPDATE_USER) && isLogin) {
            this.strictInsertFill(metaObject, UPDATE_USER, Long.class, LoginInfo.getUserId());
        }
        if (metaObject.hasGetter(INSERT_TIME)) {
            this.strictInsertFill(metaObject, INSERT_TIME, LocalDateTime.class, LocalDateTime.now());
        }
        if (metaObject.hasGetter(UPDATE_TIME)) {
            this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        }
        Boolean deleteFlag = ((BaseDo) metaObject.getOriginalObject()).getDeletedFlag();
        if (metaObject.hasGetter(DELETE_FLAG) && deleteFlag == null) {
            this.strictInsertFill(metaObject, DELETE_FLAG, Boolean.class, Boolean.FALSE);
        }

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        boolean isLogin = !(LoginInfo.getLoginInfo() == null);
        if (metaObject.hasGetter(UPDATE_USER) && isLogin) {
            this.strictUpdateFill(metaObject, UPDATE_USER, Long.class, LoginInfo.getUserId());
        }

        if (metaObject.hasGetter(UPDATE_TIME)) {
            this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        }
    }


    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        Object obj = fieldVal.get();
        if (Objects.nonNull(obj)) {
            metaObject.setValue(fieldName, obj);
        }
        return this;
    }
}