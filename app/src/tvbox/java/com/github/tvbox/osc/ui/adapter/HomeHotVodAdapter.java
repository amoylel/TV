package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.LayoutUtil;

import java.util.ArrayList;

public class HomeHotVodAdapter extends BaseQuickAdapter<Vod, BaseViewHolder> {
    private String tag = "";
    public HomeHotVodAdapter(String tag) {
        super(R.layout.item_user_hot_vod, new ArrayList<>());
        this.tag = tag;
    }

    @Override
    protected void convert(BaseViewHolder helper, Vod item) {
        ViewGroup group = (ViewGroup) helper.getView(R.id.ivThumb).getParent();
        group.getLayoutParams().width = LayoutUtil.get().getWidth();
        group.getLayoutParams().height = LayoutUtil.get().getHeight();

        TextView tvRate = helper.getView(R.id.tvRate);
        tvRate.setText(tag);

        TextView tvNote = helper.getView(R.id.tvNote);
        if (item.getVodRemarks().isEmpty()) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.getVodRemarks());
            tvNote.setVisibility(View.VISIBLE);
        }
        helper.setText(R.id.tvName, item.getVodName());
        ImgUtil.load(item.getVodPic(), helper.getView(R.id.ivThumb));
    }
}