package com.boot.study.consts.fun;

@FunctionalInterface
public interface Callable<V> {

    V execute();
}