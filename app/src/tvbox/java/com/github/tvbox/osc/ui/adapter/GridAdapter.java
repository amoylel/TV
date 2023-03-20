package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
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


public class GridAdapter extends BaseQuickAdapter<Vod, BaseViewHolder> {
    private boolean mShowList = false;

    public GridAdapter(boolean l) {
        super( l ? R.layout.item_list:R.layout.item_grid, new ArrayList<>());
        this.mShowList = l;
    }

    @Override
    protected void convert(BaseViewHolder helper, Vod item) {
        helper.setText(R.id.tvNote, item.getVodRemarks());
        helper.setText(R.id.tvName, item.getVodName());
        ImgUtil.load(item.getVodPic(), helper.getView(R.id.ivThumb));
        if(this.mShowList) return; // aist end

        ViewGroup group = (ViewGroup) helper.getView(R.id.ivThumb).getParent();
        group.getLayoutParams().width = LayoutUtil.get().getWidth();
        group.getLayoutParams().height = LayoutUtil.get().getHeight();

        TextView tvYear = helper.getView(R.id.tvYear);
        tvYear.setText(item.getVodYear());
        tvYear.setVisibility(item.getVodYear().isEmpty()?View.GONE:View.VISIBLE);
        helper.getView(R.id.tvLang).setVisibility(View.GONE);
        helper.getView(R.id.tvArea).setVisibility(View.GONE);
        helper.setVisible(R.id.tvNote, !TextUtils.isEmpty(item.getVodRemarks()));
        helper.setText(R.id.tvActor, item.getVodActor());
        ImgUtil.load(item.getVodPic(), helper.getView(R.id.ivThumb));
    }
}