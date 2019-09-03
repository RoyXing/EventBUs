package com.xingzy.eventbus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.eventbus.annotation.EventBeans;
import com.eventbus.annotation.Subscribe;
import com.eventbus.core.EventBus;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
    }

    public void jump(View view) {
        EventBus.getDefault().postSticky(new UserInfo(28, "xingzy"));
        startActivity(new Intent(this, SecondActivity.class));
    }

    @Subscribe
    public void event(UserInfo userInfo) {
        Log.e("roy", userInfo.toString());
    }

    @Subscribe(priority = 10)
    public void event2(UserInfo userInfo) {
        Log.e("roy>>> priority = 10", userInfo.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
