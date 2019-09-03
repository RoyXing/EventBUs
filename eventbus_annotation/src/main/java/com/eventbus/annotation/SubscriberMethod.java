package com.eventbus.annotation;

import java.lang.reflect.Method;

public class SubscriberMethod {

    private String methodName;
    private Method method;
    private ThreadMode threadMode;
    private Class<?> eventType;
    private int priority;
    private boolean sticky;

    public SubscriberMethod(Class subscriberClass, String methodName, Class<?> eventType, ThreadMode threadMode, int priority, boolean sticky) {
        this.methodName = methodName;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;

        try {
            method = subscriberClass.getDeclaredMethod(methodName, eventType);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public Method getMethod() {
        return method;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public Class<?> getEventType() {
        return eventType;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isSticky() {
        return sticky;
    }
}
