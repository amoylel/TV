package com.github.tvbox.osc.ui.activity;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.*;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.ActivityTvboxHomeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.github.tvbox.osc.bean.SortData;
import com.github.tvbox.osc.ui.adapter.*;
import com.github.tvbox.osc.ui.fragment.*;
import com.github.tvbox.osc.ui.tv.widget.*;
import com.github.tvbox.osc.util.*;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.*;
import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeActivity extends BaseActivity implements SiteCallback {
    private SiteViewModel sourceViewModel;
    private SortAdapter sortAdapter;
    private List<BaseLazyFragment> fragments = new ArrayList<>();
    private long mExitTime = 0;
    private ActivityTvboxHomeBinding mBinding;
    private NoScrollViewPager mViewPager;
    private Integer resID = 9999;
    private boolean isDownOrUp = false;
    public View sortFocusView = null;
    private Queue<Runnable> mDelayTasks = new LinkedList<>();
    private boolean mLoaded = false;
    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityTvboxHomeBinding.inflate(getLayoutInflater());
    }

    private void runTask(Runnable runnable) {
        synchronized (resID) {
            if (mDelayTasks != null) mDelayTasks.add(runnable);
            else App.post(runnable);
        }
    }

    @Override
    protected void initView() {
        DataLoader.get().setLoaded(false);
        DouBan.get().load(null);
        runTask(()-> waitLoading(()->{}));
        new Thread(() -> {
            Server.get().start();
            WallConfig.get().init();
            LiveConfig.get().init();
            ApiConfig.get().init().load(getCallback());
            DataLoader.get().init();
            if(!DataLoader.get().site.getName().isEmpty() && DataLoader.get().val != null) runTask(() -> onHomeContent(DataLoader.get().val, DataLoader.get().site));// 加载缓存数据
        }).start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mDelayTasks == null) return; // 首帧出来后再初始化其他控件
        synchronized (resID) {
            initViewImpl();
            initViewModel();
            while (!mDelayTasks.isEmpty()) {
                App.post(mDelayTasks.poll());
            }
            mDelayTasks = null;
        }
    }

    private void resetViewPager() {
        fragments.clear();
        sortAdapter.setNewData(new ArrayList<>());
        if (mViewPager != null) mBinding.viewPagerContenter.removeAllViews();
        mViewPager = new NoScrollViewPager(this.getActivity());
        mViewPager.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mViewPager.setId(++resID);
        mBinding.viewPagerContenter.addView(mViewPager);
    }

    private void initViewImpl() {
        mBinding.mGridView.setLayoutManager(new V7LinearLayoutManager(this.getActivity(), 0, false));
        mBinding.mGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.getActivity(), 10.0f));
        mBinding.mGridView.setAdapter(sortAdapter = new SortAdapter());
        mBinding.mGridView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                String name = (String) ((TextView) view.findViewById(R.id.tvTitle)).getText();
                SortData item = sortAdapter.findItemByName(name);
                if (name.equals(sortAdapter.getItem(0).name)) return;
                ImageView img = (ImageView) view.findViewById(R.id.tvFilter);
                img.setImageResource(R.drawable.ic_filter_on);
                img.setVisibility(sortAdapter.findItemByName(name).filters.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
            }
        });
        mBinding.mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                App.post(() -> onBlur(view, position), 10);
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                onFocus(view, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                if (itemView != null && mViewPager.getCurrentItem() == position) {
                    onItemSelect(itemView, position);
                }
            }
        });

        mBinding.mGridView.setOnInBorderKeyEventListener((direction, view) -> {
            if (direction != View.FOCUS_DOWN) return false;
            isDownOrUp = true;
            BaseLazyFragment baseLazyFragment = fragments.get(mViewPager.getCurrentItem());
            if (!(baseLazyFragment instanceof GridFragment)) return false;
            return !((GridFragment) baseLazyFragment).isLoad();
        });
        mBinding.tvName.setOnClickListener(view -> showSiteSwitch());
    }

    private void onItemSelect(View itemView, int position) {
        BaseLazyFragment baseLazyFragment = fragments.get(mViewPager.getCurrentItem());
        if (baseLazyFragment instanceof UserFragment) showSiteSwitch();
        if ((baseLazyFragment instanceof GridFragment) && !sortAdapter.getItem(position).filters.isEmpty()) { // 弹出筛选
            ((GridFragment) baseLazyFragment).showFilter(itemView);
            ((ImageView) itemView.findViewById(R.id.tvFilter)).setImageResource(R.drawable.ic_filter_off);
        }
    }

    private void onBlur(View view, int position) {
        if (view == null || isDownOrUp) return;
        if (view == mBinding.mGridView.getChildAt(mViewPager.getCurrentItem())) return;
        TextView textView = view.findViewById(R.id.tvTitle);
        textView.getPaint().setFakeBoldText(false);
        textView.setTextColor(HomeActivity.this.getResources().getColor(R.color.color_BBFFFFFF));
    }

    private void onFocus(View view, int position) {
        if (view == null) return;
        sortFocusView = view;
        isDownOrUp = false;
        TextView textView = view.findViewById(R.id.tvTitle);
        textView.getPaint().setFakeBoldText(true);
        textView.setTextColor(HomeActivity.this.getResources().getColor(R.color.color_FFFFFF));
        mViewPager.setCurrentItem(position, false);
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                mLoaded = true;
                Notify.show("配置文件加载成功");
                runTask(() -> initDataFromNet());
            }

            @Override
            public void error(int resId) {
                mLoaded = true;
                Notify.show("配置文件加载失败");
                runTask(() -> initDataFromNet());
            }
        };
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        sourceViewModel.result.observe(this, result -> onHomeContent(result, ApiConfig.get().getHome()));
    }

    private void initDataFromNet() {
        DataLoader.get().setLoaded(false);
        if(!DataLoader.get().site.getKey().equals(ApiConfig.get().getHome().getKey())) waitLoading(()->{});
        sourceViewModel.homeContent();
    }

    private void onHomeContent(Result result, Site home) {
        if(result != DataLoader.get().val || home != DataLoader.get().site) DataLoader.get().put(home, result);
        DataLoader.get().setLoaded(true);
        if(!mLoaded) App.post(()-> DataLoader.get().setLoaded(false) , 60);
        showSuccess();
        List<SortData> list = new ArrayList<>();
        if (result == null) result = new Result();
        List<Class> cls = result.getTypes();
        LinkedHashMap<String, List<Filter>> filters = result.getFilters();
        for (int i = 0; i < cls.size(); ++i) {
            SortData data = new SortData(cls.get(i).getTypeId(), cls.get(i).getTypeName(), cls.get(i).getTypeFlag());
            if (filters.containsKey(data.id)) data.filters = filters.get(data.id);
            list.add(data);
        }
        List<SortData> data = DefaultConfig.adjustSort(home.getKey(), list, true);

        do {
            if(sortAdapter == null ||sortAdapter.getItemCount() == 0) break;
            if(!sortAdapter.getData().get(0).name.equals(home.getName())) break;
            if (fragments.size() == data.size()) return; // 两次都是空数据
            if (fragments.size() > 1) return; // 已经初始化的不再重复初始化
        }while (false);

        resetViewPager();
        sortAdapter.setNewData(data);
        initViewPager(result, home.getName());
    }

    private void initViewPager(Result result, String siteName) {
        if (siteName == null || siteName.isEmpty()) siteName = "TVBox";
        if (sortAdapter.getData().size() == 0) return;
        for (SortData data : sortAdapter.getData()) {
            if (data.id.equals("my0")) {
                data.name = siteName;
                fragments.add(UserFragment.newInstance(result == null ? null : result.getList()));
            } else {
                fragments.add(GridFragment.newInstance(data));
            }
        }
        mViewPager.setPageTransformer(true, new DefaultTransformer());
        mViewPager.setAdapter(new HomePageAdapter(getSupportFragmentManager(), fragments));
        mViewPager.setCurrentItem(mViewPager.getCurrentItem(), false);
        App.post(() -> {
            View v = mBinding.mGridView.getChildAt(0);
            if (v != null) v.requestFocus();
        }, 60);
    }

    @Override
    public void onBackPressed() {
        int i = mViewPager.getCurrentItem();
        if (i == 0) {
            exit();
        } else if (this.fragments.get(i) instanceof GridFragment) {
            if (((GridFragment) this.fragments.get(i)).restoreView()) return;  // 还原上次保存的UI内容
            if (sortFocusView != null && !sortFocusView.isFocused()) {
                sortFocusView.requestFocus();
            } else {
                this.mBinding.mGridView.setSelection(0);
            }
        }
    }

    private void exit() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            finish();
            super.onBackPressed();
        } else {
            mExitTime = System.currentTimeMillis();
            Notify.show("再按一次返回键退出应用");
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (mViewPager.getCurrentItem() == 0) {
                showSiteSwitch();
            } else {
                int postion = mViewPager.getCurrentItem();
                View view = mBinding.mGridView.getChildAt(postion);
                if (view != null) onItemSelect(view, postion);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    void showSiteSwitch() {
        SiteDialog.create(this).change().show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType() == RefreshEvent.Type.VIDEO) initDataFromNet();
        if (event.getType() == RefreshEvent.Type.SIZE) {
            LayoutUtil.get().updateLayoutSize();
            initDataFromNet();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.getType()) {
            case SEARCH:
                FastSearchActivity.start(this, ApiConfig.get().getHome().getKey(), event.getText());
                break;
            case PUSH:
                if (ApiConfig.get().getSite("push_agent") == null) return;
                DetailActivity.start(this, "push_agent", event.getText(), "", true);
                break;
            default:
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        initDataFromNet();
    }

    @Override
    public void onChanged() {
    }
}