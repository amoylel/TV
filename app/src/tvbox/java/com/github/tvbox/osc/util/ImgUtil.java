package com.github.tvbox.osc.util;

import android.text.TextUtils;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.server.Server;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.squareup.picasso.Picasso;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class ImgUtil {
    public static String checkReplaceProxy(String urlOri) {
        if (urlOri.startsWith("proxy://"))
            return urlOri.replace("proxy://", Server.get().getAddress(true) + "proxy?");
        return urlOri;
    }

    public static void load(String picUrl, ImageView ivThumb) {
        if (!TextUtils.isEmpty(picUrl)) { //由于部分电视机使用glide报错
            Picasso.get()
                    .load(checkReplaceProxy(picUrl))
                    .transform(new RoundTransformation(MD5.string2MD5(picUrl))
                            .centerCorp(true)
                            .override(LayoutUtil.get().getWidth(), LayoutUtil.get().getHeight())
                            .roundRadius(AutoSizeUtils.dp2px(ivThumb.getContext(), 8), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .error(R.drawable.img_loading_placeholder)
                    .into(ivThumb);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }

    // 图片选中动画
    public static TvRecyclerView.OnItemListener animate() {
        return new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.10f).scaleY(1.10f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        };
    }
}
