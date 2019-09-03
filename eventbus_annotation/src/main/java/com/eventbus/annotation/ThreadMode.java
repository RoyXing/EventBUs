package com.eventbus.annotation;

public enum ThreadMode {

    /**
     * 订阅和发布在同一线程，避免线程切换，推荐默认模式
     */
    POSTING,

    /**
     * 主线程中被调用，切勿做耗时操作
     */
    MAIN,

    /**
     * 用于网络访问等耗时操作，事件总线已完成的异步订阅通知线程，
     * 并且使用线程池有效的复用
     */
    ASYNC,

}
