package com.github.tvbox.osc.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentGridBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.github.tvbox.osc.bean.SortData;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.dialog.GridFilterDialog;
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView;
import com.github.tvbox.osc.util.DataLoader;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.ImgUtil;
import com.kingja.loadsir.callback.Callback;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

public class GridFragment extends BaseLazyFragment {
    private SortData sortData = null;
    private TvRecyclerView mGridView;
    private SiteViewModel sourceViewModel;
    private GridFilterDialog gridFilterDialog;
    private GridAdapter gridAdapter;
    private int page = 1;
    private int maxPage = 1;
    private boolean isLoad = false;
    private View focusedView = null;

    private static class GridInfo {
        public String sortID = "";
        public TvRecyclerView mGridView;
        public GridAdapter gridAdapter;
        public int page = 1;
        public int maxPage = 1;
        public boolean isLoad = false;
        public View focusedView = null;
    }

    Stack<GridInfo> mGrids = new Stack<>(); //ui栈
    FragmentGridBinding mBinding;
    public static GridFragment newInstance(SortData sortData) {
        return new GridFragment().setArguments(sortData);
    }

    public GridFragment setArguments(SortData sortData) {
        this.sortData = sortData;
        return this;
    }

    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentGridBinding.inflate(inflater, container, false);
    }
    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void changeView(String id) {
        initView();
        this.sortData.id = id; // 修改sortData.id为新的ID
        initViewModel();
        initData();
    }

    public boolean isFolderMode() {
        return (getUITag() == '1');
    }

    // 获取当前页面UI的显示模式 ‘0’ 正常模式 '1' 文件夹模式 '2' 显示缩略图的文件夹模式
    public char getUITag() {
        return (sortData == null || sortData.flag == null || sortData.flag.length() == 0) ? '0' : sortData.flag.charAt(0);
    }

    // 是否允许聚合搜索 sortData.flag的第二个字符为‘1’时允许聚搜
    public boolean enableFastSearch() {
        return (sortData.flag == null || sortData.flag.length() < 2) ? true : (sortData.flag.charAt(1) == '1');
    }

    // 保存当前页面
    private void saveCurrentView() {
        if (this.mGridView == null) return;
        GridInfo info = new GridInfo();
        info.sortID = this.sortData.id;
        info.mGridView = this.mGridView;
        info.gridAdapter = this.gridAdapter;
        info.page = this.page;
        info.maxPage = this.maxPage;
        info.isLoad = this.isLoad;
        info.focusedView = this.focusedView;
        this.mGrids.push(info);
    }

    // 丢弃当前页面，将页面还原成上一个保存的页面
    public boolean restoreView() {
        if (mGrids.empty()) return false;
        this.showSuccess();
        ((ViewGroup) mGridView.getParent()).removeView(this.mGridView); // 重父窗口移除当前控件
        GridInfo info = mGrids.pop();// 还原上次保存的控件
        this.sortData.id = info.sortID;
        this.mGridView = info.mGridView;
        this.gridAdapter = info.gridAdapter;
        this.page = info.page;
        this.maxPage = info.maxPage;
        this.isLoad = info.isLoad;
        this.focusedView = info.focusedView;
        this.mGridView.setVisibility(View.VISIBLE);
//        if(this.focusedView != null){ this.focusedView.requestFocus(); }
        if (mGridView != null) mGridView.requestFocus();
        return true;
    }

    // 更改当前页面
    private void createView() {
        this.saveCurrentView(); // 保存当前页面
        if (mGridView == null) { // 从layout中拿view
            mGridView = findViewById(R.id.mGridView);
        } else { // 复制当前view
            TvRecyclerView v3 = new TvRecyclerView(this.mContext);
            v3.setSpacingWithMargins(10, 10);
            v3.setLayoutParams(mGridView.getLayoutParams());
            v3.setPadding(mGridView.getPaddingLeft(), mGridView.getPaddingTop(), mGridView.getPaddingRight(), mGridView.getPaddingBottom());
            v3.setClipToPadding(mGridView.getClipToPadding());
            ((ViewGroup) mGridView.getParent()).addView(v3);
            mGridView.setVisibility(View.GONE);
            mGridView = v3;
            mGridView.setVisibility(View.VISIBLE);
        }
        mGridView.setHasFixedSize(true);
        gridAdapter = new GridAdapter(isFolderMode());
        this.page = 1;
        this.maxPage = 1;
        this.isLoad = false;
    }

    private void initView() {
        this.createView();
        mGridView.setAdapter(gridAdapter);
        if (isFolderMode()) {
            mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
        } else {
            mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, Product.getColumn()));
        }

        gridAdapter.setOnLoadMoreListener(() -> {
            gridAdapter.setEnableLoadMore(true);
            sourceViewModel.categoryContent(ApiConfig.get().getHome().getKey(), sortData.id, page + "", true, sortData.filterSelect);
        }, mGridView);
        mGridView.setOnItemListener(ImgUtil.animate());
        gridAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Vod video = gridAdapter.getData().get(position);
            if (video == null) return;
            if (("12".indexOf(getUITag()) != -1) && video.getVodTag().equals("folder")) {
                focusedView = view;
                changeView(video.getVodId());
            } else if (video.getVodId().isEmpty() || video.getVodId().startsWith("msearch:")) {
                FastSearchActivity.start(getActivity(), ApiConfig.get().getHome().getKey(), video.getVodName());
            } else {
                DetailActivity.start(getActivity(), ApiConfig.get().getHome().getKey(), video.getVodId(), video.getVodName());
            }
        });
        gridAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Vod video = gridAdapter.getData().get(position);
            if (video != null) FastSearchActivity.start(getActivity(), ApiConfig.get().getHome().getKey(), video.getVodName());
            return true;
        });
        gridAdapter.setLoadMoreView(new LoadMoreView());
        setLoadSir2(mGridView);
    }

    private void initViewModel() {
        if (sourceViewModel != null) return;
        sourceViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        sourceViewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getList().size() > 0) {
                if (page == 1) {
                    showSuccess();
                    isLoad = true;
                    gridAdapter.setNewData(result.getList());
                } else {
                    gridAdapter.addData(result.getList());
                }
                page++;
                maxPage = page;

                if (page > maxPage || isFolderMode()) {
                    gridAdapter.loadMoreEnd();
                    gridAdapter.setEnableLoadMore(false);
                } else {
                    gridAdapter.loadMoreComplete();
                    gridAdapter.setEnableLoadMore(true);
                }
            } else {
                if (page == 1) showEmpty();
                if (page > maxPage) {
                    Toast.makeText(getContext(), "没有更多了", Toast.LENGTH_SHORT).show();
                    gridAdapter.loadMoreEnd();
                }else{
                    gridAdapter.loadMoreComplete();
                }
                gridAdapter.setEnableLoadMore(false);
            }
        });
    }

    public boolean isLoad() {
        return isLoad || !mGrids.empty(); //如果有缓存页的话也可以认为是加载了数据的
    }

    private void initData() {
        showLoading();
        isLoad = false;
        mGridView.scrollToPosition(0);
        waitLoading(()->sourceViewModel.categoryContent(ApiConfig.get().getHome().getKey(), sortData.id, page + "", true, sortData.filterSelect));
    }

    // 等待配置文件加载完成
    @Override
    public void waitLoading(Runnable runnable){
        if(!DataLoader.get().isLoaded()){
            App.post(()->waitLoading(runnable), 10);
        } else {
            App.post(runnable);
        }
    }

    public void showFilter(View view) {
        if (!sortData.filters.isEmpty() && gridFilterDialog == null) {
            gridFilterDialog = new GridFilterDialog(mContext);
            gridFilterDialog.setData(sortData);
            gridFilterDialog.setOnDismiss((c) -> {
                if(c){
                    page = 1;
                    initData();
                }else{
                    ImageView img = view.findViewById(R.id.tvFilter);
                    img.setImageResource(R.drawable.ic_filter_on);
                }
            });
        }
        if (gridFilterDialog != null) gridFilterDialog.show();
    }
}