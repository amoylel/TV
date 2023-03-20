package com.github.tvbox.osc.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentUserBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.adapter.HomeHotVodAdapter;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.PushActivity;
import com.fongmi.android.tv.ui.activity.SettingActivity;
import com.github.tvbox.osc.util.DouBan;
import com.github.tvbox.osc.util.ImgUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class UserFragment extends BaseLazyFragment implements View.OnClickListener, BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {
    private HomeHotVodAdapter homeSiteVodAdapter;
    private HomeHotVodAdapter homeHotVodAdapter;
    private List<Vod> homeSourceRec;
    FragmentUserBinding mBinding;

    private View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
        float scale = hasFocus ? 1.10f : 1.0f;
        v.animate().scaleX(scale).scaleY(scale).setDuration(300).setInterpolator(new BounceInterpolator()).start();
    };

    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentUserBinding.inflate(inflater, container, false);
    }

    public static UserFragment newInstance(List<Vod> recVod) {
        return new UserFragment().setArguments(recVod);
    }

    public UserFragment setArguments(List<Vod> recVod) {
        homeSourceRec = recVod;
        if(homeSourceRec == null) homeSourceRec = new ArrayList<>();
        return this;
    }

    @Override
    protected void onFragmentResume() {
        mBinding.tvHotList1.setVisibility(View.VISIBLE);
        mBinding.tvHotList2.setVisibility(View.VISIBLE);
        super.onFragmentResume();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_user;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        mBinding.tvLive.setOnClickListener(this);
        mBinding.tvSearch.setOnClickListener(this);
        mBinding.tvSetting.setOnClickListener(this);
        mBinding.tvHistory.setOnClickListener(this);
        mBinding.tvPush.setOnClickListener(this);
        mBinding.tvFavorite.setOnClickListener(this);
        mBinding.tvLive.setOnFocusChangeListener(focusChangeListener);
        mBinding.tvSearch.setOnFocusChangeListener(focusChangeListener);
        mBinding.tvSetting.setOnFocusChangeListener(focusChangeListener);
        mBinding.tvHistory.setOnFocusChangeListener(focusChangeListener);
        mBinding.tvPush.setOnFocusChangeListener(focusChangeListener);
        mBinding.tvFavorite.setOnFocusChangeListener(focusChangeListener);
        homeHotVodAdapter = new HomeHotVodAdapter("豆瓣热播");
        homeHotVodAdapter.setOnItemClickListener(this::onItemClick);
        homeHotVodAdapter.setOnItemLongClickListener(this::onItemLongClick);
        mBinding.tvHotList1.setOnItemListener(ImgUtil.animate());
        mBinding.tvHotList1.setAdapter(homeHotVodAdapter);
        homeHotVodAdapter.setNewData(DouBan.get().load(movies -> homeHotVodAdapter.setNewData(movies == null ? new ArrayList<>(): movies)));

        homeSiteVodAdapter = new HomeHotVodAdapter(ApiConfig.get().getHome().getName());
        homeSiteVodAdapter.setOnItemClickListener(this::onItemClick);
        homeSiteVodAdapter.setOnItemLongClickListener(this::onItemLongClick);
        mBinding.tvHotList2.setOnItemListener(ImgUtil.animate());
        mBinding.tvHotList2.setAdapter(homeSiteVodAdapter);
        homeSiteVodAdapter.setNewData(this.homeSourceRec);
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        waitLoading(()->{
            if (ApiConfig.get().getSites().isEmpty()) return;
            Vod vod = (Vod) adapter.getItem(position);
            if (vod.getVodId().isEmpty() || vod.getVodId().startsWith("msearch:")) {
                FastSearchActivity.start(getActivity(),ApiConfig.get().getHome().getKey(), vod.getVodName());
            } else {
                DetailActivity.start(getActivity(), ApiConfig.get().getHome().getKey(), vod.getVodId(), vod.getVodName());
            }
        });
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        waitLoading(()->{
            if (ApiConfig.get().getSites().isEmpty()) return ;
            Vod vod = (Vod) adapter.getItem(position);
            FastSearchActivity.start(getActivity(),ApiConfig.get().getHome().getKey(), vod.getVodName());
        });
        return true;
    }


    @Override
    public void onClick(View v) {
        waitLoading(()->{
            if (v.getId() == R.id.tvLive) LiveActivity.start(getActivity());
            if (v.getId() == R.id.tvSearch) SearchActivity.start(getActivity());
            if (v.getId() == R.id.tvSetting) SettingActivity.start(getActivity());
            if (v.getId() == R.id.tvHistory) HistoryActivity.start(getActivity());
            if (v.getId() == R.id.tvPush) PushActivity.start(getActivity());
            if (v.getId() == R.id.tvFavorite) CollectActivity.start(getActivity());
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}