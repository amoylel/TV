package com.github.tvbox.osc.ui.activity;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityTvboxHomeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.utils.Notify;
import com.github.tvbox.osc.bean.SortData;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.fragment.BaseLazyFragment;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.DataLoader;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.DouBan;
import com.github.tvbox.osc.util.LayoutUtil;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeActivity extends BaseActivity {
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
        setLoadSir(mBinding.contentLayout);
        showLoading();
        DataLoader.get().setLoaded(false);
        DouBan.get().load(null);
        new Thread(() -> {
            Server.get().start();
            WallConfig.get().init();
            LiveConfig.get().init();
            ApiConfig.get().init().load(getCallback());
            DataLoader.get().init();
            runTask(() -> initDataFromCache());// 加载缓存数据
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
                    BaseLazyFragment baseLazyFragment = fragments.get(mViewPager.getCurrentItem());
                    if (baseLazyFragment instanceof UserFragment) showSiteSwitch();
                    if ((baseLazyFragment instanceof GridFragment) && !sortAdapter.getItem(position).filters.isEmpty()) { // 弹出筛选
                        ((GridFragment) baseLazyFragment).showFilter(itemView);
                        ((ImageView) itemView.findViewById(R.id.tvFilter)).setImageResource(R.drawable.ic_filter_off);
                    }
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
                runTask(() -> initDataFromNet(false));
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                runTask(() -> initDataFromEmpty());
            }
        };
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        sourceViewModel.result.observe(this, result -> onHomeContent(result, true));
    }

    private void initDataFromCache() {
        if (!fragments.isEmpty()) return;
        resetViewPager();
        onHomeContent(DataLoader.get().val, false);
    }

    private void initDataFromEmpty() {
        DataLoader.get().setLoaded(true);
        showSuccess();
        if (sortAdapter != null && sortAdapter.getItemCount() == 1 && sortAdapter.getItem(0).name == "TVBox")
            return;
        resetViewPager();
        onHomeContent(null, true);
    }

    private void initDataFromNet(boolean clear) {
        DataLoader.get().setLoaded(false);   // 将加载状态标记为未完成
        if (clear) {
            showLoading(); //
            DataLoader.get().clear();
        }
        if (mViewPager == null || clear) resetViewPager();
        sourceViewModel.homeContent();
    }

    private void onHomeContent(Result result, boolean refresh) {
        if (refresh) runTask(() -> DataLoader.get().setLoaded(true));
        if (refresh && DataLoader.get().refresh(ApiConfig.get().getHome(), result)) { // 判断是否需要重新初始化当前数据
            runTask(() -> initDataFromCache());
            return;
        }
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
        List<SortData> data = DefaultConfig.adjustSort(DataLoader.get().site.getKey(), list, true);
        if (fragments.size() == data.size() && data.size() == 1) return; // 两次都是空数据
        if (fragments.size() > 1) return; // 已经初始化的不再重复初始化
        sortAdapter.setNewData(data);
        initViewPager(result, DataLoader.get().site.getName());
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
            if(v != null) v.requestFocus();
        } , 60);
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
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) showSiteSwitch();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void showSiteSwitch() {
        List<Site> sites = ApiConfig.get().getSites();
        if (sites.size() > 0) {
            SelectDialog<Site> dialog = new SelectDialog<>(HomeActivity.this);
            dialog.setTip("请选择首页数据源");
            dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Site>() {
                @Override
                public void click(Site value, int pos) {
                    ApiConfig.get().setHome(value);
                    initDataFromNet(true);
                    dialog.hide();
                }

                @Override
                public String getDisplay(Site val) {
                    return val.getName();
                }
            }, new DiffUtil.ItemCallback<Site>() {
                @Override
                public boolean areItemsTheSame(@NonNull @NotNull Site oldItem, @NonNull @NotNull Site newItem) {
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(@NonNull @NotNull Site oldItem, @NonNull @NotNull Site newItem) {
                    return oldItem.getKey().equals(newItem.getKey());
                }
            }, sites, sites.indexOf(ApiConfig.get().getHome()));
            dialog.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType() == RefreshEvent.Type.VIDEO)  initDataFromNet(true);
        if (event.getType() == RefreshEvent.Type.SIZE) {
            LayoutUtil.get().updateLayoutSize();
            initDataFromNet(true);
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
}