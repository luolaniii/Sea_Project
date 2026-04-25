package com.boot.study.utils;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;


@Slf4j
@Component
public class TransactionalUtil {

    private static TransactionTemplate TRANSACTION_TEMPLATE;

    public TransactionalUtil(TransactionTemplate transactionUtil) {
        TRANSACTION_TEMPLATE = transactionUtil;
    }

    /**
     * 有返回值
     */
    public static <T> T transactionCallback(Supplier<T> supplier) {
        return TRANSACTION_TEMPLATE.execute(transactionStatus -> {
            try {
                //执行业务
                return supplier.get();
            } catch (Exception e) {
                log.error("有返回值，Exception,{}", e.getMessage(), e);
                //回滚
                transactionStatus.setRollbackOnly();
                throw e;
            }
        });
    }

    /**
     * 无返回值的
     */
    public static void transactionCallback(Runnable runnable) {
        TRANSACTION_TEMPLATE.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus transactionStatus) {
                try {
                    //业务代码
                    runnable.run();
                } catch (Exception e) {
                    log.error("无返回值，Exception,{}", e.getMessage(), e);
                    //回滚
                    transactionStatus.setRollbackOnly();
                    throw e;
                }
            }
        });
    }
}
