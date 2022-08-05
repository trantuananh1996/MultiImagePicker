package com.zhihu.matisse.internal.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public  abstract class AppActivityObserver implements DefaultLifecycleObserver {

    protected abstract void update();

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onCreate(owner);
        owner.getLifecycle().removeObserver(this);
        update();
    }
}
