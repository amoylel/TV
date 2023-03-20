package com.github.tvbox.osc.ui.adapter;

import android.view.ViewGroup;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Vod;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.LayoutUtil;

import java.util.ArrayList;

public class FastSearchAdapter extends BaseQuickAdapter<Vod, BaseViewHolder> {
    public FastSearchAdapter() {
        super(R.layout.item_search, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Vod item) {
        ViewGroup group = (ViewGroup) helper.getView(R.id.ivThumb).getParent();
        group.getLayoutParams().width = LayoutUtil.get().getWidth(220);
        group.getLayoutParams().height = LayoutUtil.get().getHeight(220);
        // with preview
        helper.setText(R.id.tvName, item.getVodName());
        helper.setText(R.id.tvSite, ApiConfig.get().getSite(item.getSiteKey()).getName());
        helper.setVisible(R.id.tvNote, item.getVodRemarks() != null && !item.getVodRemarks().isEmpty());
        if (item.getVodRemarks() != null && !item.getVodRemarks().isEmpty()) {
            helper.setText(R.id.tvNote, item.getVodRemarks());
        }
        ImgUtil.load(item.getVodPic(), helper.getView(R.id.ivThumb));
    }
}