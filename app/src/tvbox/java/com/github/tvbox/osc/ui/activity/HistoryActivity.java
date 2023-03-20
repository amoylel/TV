package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;


import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.util.ImgUtil;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import java.util.List;

import androidx.viewbinding.ViewBinding;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HistoryActivity extends BaseActivity {
    private HistoryAdapter historyAdapter;
    private boolean delMode = false;
    private ActivityHistoryBinding mBinding;
    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }
    @Override
    protected ViewBinding getBinding() { return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater()); }

    private void toggleDelMode() {
        delMode = !delMode;
        mBinding.tvDelTip.setVisibility(delMode ? View.VISIBLE : View.GONE);
        mBinding.tvDel.setTextColor(delMode ? getResources().getColor(R.color.color_FF0057) : Color.WHITE);
    }
    @Override
    protected void initView() {
        initViewImpl();
        initData();
    }

    protected void initViewImpl() {
        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7GridLayoutManager(this, Product.getColumn()));
        mBinding.mGridView.setAdapter(historyAdapter = new HistoryAdapter());
        mBinding.tvDel.setOnClickListener(view -> toggleDelMode());
        mBinding.mGridView.setOnInBorderKeyEventListener((direction, focused) -> {
            if (direction == View.FOCUS_UP) {
                mBinding.tvDel.setFocusable(true);
                mBinding.tvDel.requestFocus();
            }
            return false;
        });

        mBinding.mGridView.setOnItemListener(ImgUtil.animate());
        historyAdapter.setOnItemClickListener((adapter, view, position) -> {
            History vodInfo = historyAdapter.getData().get(position);
            if (vodInfo == null) return;
            if (delMode) {
                historyAdapter.remove(position);
                vodInfo.delete();
            } else {
                DetailActivity.start(getActivity(), vodInfo.getSiteKey(), vodInfo.getVodId(), vodInfo.getVodName());
            }
        });
    }

    private void initData() {
        List<History> items = History.get();
        historyAdapter.setNewData(items);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.HISTORY) initData();
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