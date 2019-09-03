package com.eventbus.annotation;

public interface SubscriberInfo {

    /**
     * 订阅所属类
     * @return
     */
    Class<?> getSubscriberClass();

    /**
     * 获取订阅所属类中所有订阅事件的方法
     * @return
     */
    SubscriberMethod[] getSubscriberMethods();
}
