package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityTvboxCollectBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.ImgUtil;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class CollectActivity extends BaseActivity {
    private CollectAdapter collectAdapter;
    private boolean delMode = false;
    private ActivityTvboxCollectBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, CollectActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityTvboxCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        initViewImpl();
        initData();
    }

    private void toggleDelMode() {
        delMode = !delMode;
        mBinding.tvDelTip.setVisibility(delMode ? View.VISIBLE : View.GONE);
        mBinding.tvDel.setTextColor(delMode ? getResources().getColor(R.color.color_FF0057) : Color.WHITE);
    }

    protected void initViewImpl() {
        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7GridLayoutManager(this.getActivity(), Product.getColumn()));
        mBinding.mGridView.setAdapter(collectAdapter = new CollectAdapter());
        mBinding.tvDel.setOnClickListener(view -> toggleDelMode());

        mBinding.mGridView.setOnInBorderKeyEventListener((direction, focused) -> {
            if (direction == View.FOCUS_UP) {
                mBinding.tvDel.setFocusable(true);
                mBinding.tvDel.requestFocus();
            }
            return false;
        });
        mBinding.mGridView.setOnItemListener(ImgUtil.animate());
        collectAdapter.setOnItemClickListener((adapter, view, position) ->   {
            FastClickCheckUtil.check(view);
            Keep vodInfo = collectAdapter.getData().get(position);
            if (vodInfo != null) {
                if (delMode) {
                    collectAdapter.remove(position);
                    Keep.delete(vodInfo.getCid());
                } else {
                    if (ApiConfig.get().getSite(vodInfo.getSiteKey()) != null) {
                        DetailActivity.start(getActivity(), vodInfo.getSiteKey(), vodInfo.getVodId(), vodInfo.getVodName());
                    } else {
                        FastSearchActivity.start(getActivity(), vodInfo.getSiteKey(), vodInfo.getVodName());
                    }
                }
            }
        });
    }

    private void initData() {
        List<Keep> allVodRecord = Keep.getVod();
        List<Keep> vodInfoList = new ArrayList<>();
        for (Keep vodInfo : allVodRecord) {
            vodInfoList.add(vodInfo);
        }
        collectAdapter.setNewData(vodInfoList);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.KEEP) initData();
    }

    @Override
    public void onBackPressed() {
        if (delMode) {
            toggleDelMode();
            return;
        }
        super.onBackPressed();
    }
}