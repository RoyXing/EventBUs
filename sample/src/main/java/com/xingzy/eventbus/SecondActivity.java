package com.xingzy.eventbus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.eventbus.annotation.Subscribe;
import com.eventbus.core.EventBus;

public class SecondActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        EventBus.getDefault().register(this);
    }

    @Subscribe(sticky = true)
    public void event(UserInfo userInfo) {
        Log.e("roy>>>", userInfo.toString());
    }

    public void back(View view) {
        EventBus.getDefault().post(new UserInfo(26, "邢正一"));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserInfo userInfo = EventBus.getDefault().getStickyEvent(UserInfo.class);
        if (userInfo != null) {
            UserInfo info = EventBus.getDefault().removeStickyEvent(UserInfo.class);
            if (info != null) {
                EventBus.getDefault().removeAllStickyEvents();
            }
        }

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
