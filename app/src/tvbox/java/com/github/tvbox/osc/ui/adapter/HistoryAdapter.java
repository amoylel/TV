package com.github.tvbox.osc.ui.adapter;

import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.LayoutUtil;

import java.util.ArrayList;

public class HistoryAdapter extends BaseQuickAdapter<History, BaseViewHolder> {
    public HistoryAdapter() {
        super(R.layout.item_grid, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, History item) {
        ViewGroup group = (ViewGroup) helper.getView(R.id.ivThumb).getParent();
        group.getLayoutParams().width = LayoutUtil.get().getWidth();
        group.getLayoutParams().height = LayoutUtil.get().getHeight();

        TextView tvYear = helper.getView(R.id.tvYear);
        tvYear.setText(ApiConfig.get().getSite(item.getSiteKey()).getName());
        helper.setVisible(R.id.tvLang, false);
        helper.setVisible(R.id.tvArea, false);
        if (item.getVodRemarks().isEmpty()) {
            helper.setVisible(R.id.tvNote, false);
        } else {
            helper.setText(R.id.tvNote,  "上次看到:" + item.getVodRemarks());
        }
        helper.setText(R.id.tvName, item.getVodName());
        ImgUtil.load(item.getVodPic(), helper.getView(R.id.ivThumb));
    }
}