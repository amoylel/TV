package com.github.tvbox.osc.ui.activity;

import android.os.Bundle;
import android.view.View;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Notify;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.util.DataLoader;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.NoEncryption;

public abstract class BaseActivity extends com.fongmi.android.tv.ui.base.BaseActivity {
    private static boolean initLoadSir = false;
    private LoadService mLoadService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!initLoadSir) {
            initLoadSir = true;
            LoadSir.beginBuilder().addCallback(new EmptyCallback()).addCallback(new LoadingCallback()).commit();
            Hawk.init(this).setEncryption(new NoEncryption()).build();
        }
        super.onCreate(savedInstanceState);
    }

    protected void setLoadSir(View view) {
        if (mLoadService != null) return;
        mLoadService = LoadSir.getDefault().register(view, (Callback.OnReloadListener) v -> {});
    }

    protected void showLoading() {
        if (mLoadService != null) mLoadService.showCallback(LoadingCallback.class);
    }

    protected void showEmpty() {
        if (mLoadService != null) mLoadService.showCallback(EmptyCallback.class);
    }

    protected void showSuccess() {
        if (null != mLoadService) mLoadService.showSuccess();
    }

    public void waitLoading(Runnable rb, boolean sh){
        if(DataLoader.get().isLoaded()){
            Notify.dismiss();
            App.post(rb);
        }else {
            if(!sh) Notify.progress(this);
            App.post(()-> waitLoading(rb, true), 20);
        }
    }

    public void waitLoading(Runnable rb){
        waitLoading(rb, false);
    }
}
