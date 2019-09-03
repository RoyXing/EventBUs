package com.xingzy.eventbus;

import android.app.Application;

import com.eventbus.core.EventBus;
import com.xingzy.eventbus.apt.EventBusIndex;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().addIndex(new EventBusIndex());
    }
}
