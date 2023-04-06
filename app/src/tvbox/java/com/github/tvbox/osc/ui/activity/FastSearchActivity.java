package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityFastSearchBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.FastListAdapter;
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter;
import com.github.tvbox.osc.ui.adapter.SearchWordAdapter;
import com.github.tvbox.osc.util.ImgUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.lzy.okgo.OkGo;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;

public class FastSearchActivity extends BaseActivity {
    SiteViewModel sourceViewModel;
    private SearchWordAdapter searchWordAdapter;
    private FastSearchAdapter searchAdapter;
    private FastSearchAdapter searchAdapterFilter;
    private FastListAdapter spListAdapter;
    private String searchTitle = "";
    private HashMap<String, String> spNames;
    private boolean isFilterMode = false;
    private String searchFilterKey = "";    // 过滤的key
    private HashMap<String, ArrayList<Vod>> resultVods; // 搜索结果
    private int finishedCount = 0;
    private final List<String> quickSearchWord = new ArrayList<>();
    private HashMap<String, String> mCheckSources = null;
    private ActivityFastSearchBinding mBinding;

    public static HashMap<String, String> getSourcesForSearch() {
        HashMap<String, String> mCheckSources = new HashMap<>();
        for (Site bean : ApiConfig.get().getSites()) {
            if (!bean.isSearchable()) {
                continue;
            }
            mCheckSources.put(bean.getKey(), "1");
        }
        return mCheckSources;
    }

    public static List<String> splitWords(String text) {
        List<String> result = new ArrayList<>();
        result.add(text);
        String[] parts = text.split("\\W+");
        if (parts.length > 1) {
            result.addAll(Arrays.asList(parts));
        }
        return result;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityFastSearchBinding.inflate(getLayoutInflater());
    }

    public static void start(Activity activity, String key, String name) {
        if (name == null || name.isEmpty()) return;
        Intent intent = new Intent(activity, FastSearchActivity.class);
        intent.putExtra("sourceKey", key);
        intent.putExtra("title", name);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }

    private final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View itemView, boolean hasFocus) {
            try {
                if (!hasFocus) {
                    spListAdapter.onLostFocus(itemView);
                } else {
                    int ret = spListAdapter.onSetFocus(itemView);
                    if (ret < 0) return;
                    TextView v = (TextView) itemView;
                    String sb = v.getText().toString();
                    filterResult(sb);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(FastSearchActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }

        }
    };


    private List<Runnable> pauseRunnable = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (pauseRunnable != null && pauseRunnable.size() > 0) {
            searchExecutorService = Executors.newFixedThreadPool(5);
            allRunCount.set(pauseRunnable.size());
            for (Runnable runnable : pauseRunnable) {
                searchExecutorService.execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }
    }

    @Override
    protected void initView() {
        spNames = new HashMap<>();
        resultVods = new HashMap<>();
        initViewImpl();
        initViewModel();
        initData();
    }

    public void callDetailActivity(Vod video, View view, boolean longClick) {
//        FastClickCheckUtil.check(view);
        if (video != null) {
            try {
                if (searchExecutorService != null) {
                    pauseRunnable = searchExecutorService.shutdownNow();
                    searchExecutorService = null;
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
            DetailActivity.start(getActivity(), video.getSiteKey(), video.getVodId(), video.getVodName(), longClick);
        }
    }


    protected void initViewImpl() {
        mBinding.mGridViewWord.setHasFixedSize(true);
        mBinding.mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        spListAdapter = new FastListAdapter();
        mBinding.mGridViewWord.setAdapter(spListAdapter);
        mBinding.mGridViewWord.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View child) {
                child.setOnFocusChangeListener(focusChangeListener);
                TextView t = (TextView) child;
                if (t.getText() == "全部显示") {
                    t.requestFocus();
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                view.setOnFocusChangeListener(null);
            }
        });

        spListAdapter.setOnItemClickListener((adapter, view, position) -> {
            filterResult(spListAdapter.getItem(position));
        });

        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7GridLayoutManager(this, Product.getColumn()));
        mBinding.mGridView.setOnItemListener(ImgUtil.animate());
        searchAdapter = new FastSearchAdapter();
        mBinding.mGridView.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener((adapter, view, position) -> callDetailActivity(searchAdapter.getData().get(position), view, false));
        mBinding.mGridViewFilter.setLayoutManager(new V7GridLayoutManager(this, Product.getColumn()));
        mBinding.mGridViewFilter.setOnItemListener(ImgUtil.animate());
        searchAdapterFilter = new FastSearchAdapter();
        mBinding.mGridViewFilter.setAdapter(searchAdapterFilter);
        searchAdapterFilter.setOnItemClickListener((adapter, view, position) -> callDetailActivity(searchAdapterFilter.getData().get(position), view, false));

        setLoadSir(mBinding.llLayout);

        // 分词
        searchWordAdapter = new SearchWordAdapter();
        mBinding.mGridViewWordFenci.setAdapter(searchWordAdapter);
        mBinding.mGridViewWordFenci.setLayoutManager(new V7LinearLayoutManager(this.getActivity(), 0, false));
        searchWordAdapter.setOnItemClickListener((adapter, view, position) -> {
            String str = searchWordAdapter.getData().get(position);
            search(str);
        });
        searchWordAdapter.setNewData(new ArrayList<>());
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        sourceViewModel.search.observe(this, result -> {
            onSearchResult(result);
            mBinding.mSearchTitle.setText(String.format("搜索(%d/%d/%d)", resultVods.size(), finishedCount, spNames.size()));
        });
    }

    private void filterResult(String spName) {
        if (spName.equals("全部显示")) {
            mBinding.mGridView.setVisibility(View.VISIBLE);
            mBinding.mGridViewFilter.setVisibility(View.GONE);
            return;
        }
        mBinding.mGridView.setVisibility(View.GONE);
        mBinding.mGridViewFilter.setVisibility(View.VISIBLE);
        String key = spNames.get(spName);
        if (key.isEmpty()) return;

        if (searchFilterKey == key) return;
        searchFilterKey = key;

        List<Vod> list = resultVods.get(key);
        searchAdapterFilter.setNewData(list);
    }

    private void fenci() {
        if (!quickSearchWord.isEmpty()) return; // 如果经有分词了，不再进行二次分词
        // 分词
        OkHttp.newCall(String.format("http://api.pullword.com/get.php?source=%s&param1=0&param2=0&json=1", URLEncoder.encode(searchTitle))).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                try {
                    String json = response.body().string();
                    quickSearchWord.clear();
                    for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
                        quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
                    }
                    quickSearchWord.addAll(splitWords(searchTitle));
                    List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
                } catch (Exception e) {
                }
            }
        });
    }

    private void initData() {
        initCheckedSourcesForSearch();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            search(title);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD) {
            if (event.obj != null) {
                List<String> data = (List<String>) event.obj;
                searchWordAdapter.setNewData(data);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = getSourcesForSearch();
    }

    private void search(String title) {
        cancel();
        showLoading();
        this.searchTitle = title;
        fenci();
        mBinding.mGridView.setVisibility(View.INVISIBLE);
        mBinding.mGridViewFilter.setVisibility(View.GONE);
        searchAdapter.setNewData(new ArrayList<>());
        searchAdapterFilter.setNewData(new ArrayList<>());

        spListAdapter.reset();
        resultVods.clear();
        searchFilterKey = "";
        isFilterMode = false;
        spNames.clear();
        finishedCount = 0;
        searchResult();
    }

    private ExecutorService searchExecutorService = null;
    private AtomicInteger allRunCount = new AtomicInteger(0);

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            searchAdapter.setNewData(new ArrayList<>());
            searchAdapterFilter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }
        searchExecutorService = Executors.newFixedThreadPool(6);
        List<Site> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSites());
        Site home = ApiConfig.get().getHome();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);


        ArrayList<String> siteKey = new ArrayList<>();
        ArrayList<String> hots = new ArrayList<>();

        spListAdapter.setNewData(hots);
        spListAdapter.addData("全部显示");
        for (Site bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            this.spNames.put(bean.getName(), bean.getKey());
            allRunCount.incrementAndGet();
        }
        updateSearchResultCount(0);
        for (String key : siteKey) {
            searchExecutorService.execute(() -> {
                try {
                    sourceViewModel.searchContent(ApiConfig.get().getSite(key), searchTitle);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                updateSearchResultCount(1);
            });
        }
    }

    synchronized private void updateSearchResultCount(int n) {
        finishedCount += n;
        if (finishedCount > spNames.size()) finishedCount = spNames.size();

    }

    // 向过滤栏添加有结果的spname
    private String addWordAdapterIfNeed(String key) {
        try {
            String name = "";
            for (String n : spNames.keySet()) {
                if (spNames.get(n) == key) {
                    name = n;
                }
            }
            if (name == "") return key;

            List<String> names = spListAdapter.getData();
            for (int i = 0; i < names.size(); ++i) {
                if (name == names.get(i)) {
                    return key;
                }
            }

            spListAdapter.addData(name);
            return key;
        } catch (Exception e) {
            return key;
        }
    }

    private boolean matchSearchResult(String name, String searchTitle) {
        if(name == null || name.isEmpty()) return false;
        if(searchTitle == null || searchTitle.isEmpty()) return false;
        searchTitle = searchTitle.trim();
        String[] arr = searchTitle.split("\\s+");
        int matchNum = 0;
        for(int i =0; i < searchTitle.length(); ++i){
            if(name.indexOf(searchTitle.charAt(i) )!= -1) matchNum++;
        }
        return (matchNum >= Math.max(1, arr.length / 2)) ? true : false;
    }

    private void onSearchResult(Result result) {
        String lastSourceKey = "";

        if (result != null && result.getList().size() > 0) {
            List<Vod> data = new ArrayList<>();
            for (Vod video : result.getList()) {
                if (!matchSearchResult(video.getVodName(), searchTitle)) continue;
                data.add(video);
                if (!resultVods.containsKey(video.getSiteKey())) {
                    resultVods.put(video.getSiteKey(), new ArrayList<>());
                }
                resultVods.get(video.getSiteKey()).add(video);
                if (video.getSiteKey() != lastSourceKey) {
                    lastSourceKey = this.addWordAdapterIfNeed(video.getSiteKey());
                }
            }

            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                if (!isFilterMode)
                    mBinding.mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
            }
            cancel();
        }
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}