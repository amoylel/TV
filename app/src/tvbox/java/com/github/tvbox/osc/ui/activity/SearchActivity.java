package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.databinding.ActivityTvboxSearchBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.github.tvbox.osc.ui.adapter.PinyinAdapter;
import com.github.tvbox.osc.ui.adapter.SearchHistoryAdapter;
import com.github.tvbox.osc.ui.dialog.RemoteDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;

public class SearchActivity extends BaseActivity {
    private SearchHistoryAdapter historyAdapter;
    private PinyinAdapter wordAdapter;
    private PinyinAdapter wordIQYAdapter;
    private ActivityTvboxSearchBinding mBinding;

    private static ArrayList<String> hotQQ;
    private static ArrayList<String> hotIQY;

    private ExecutorService executor = Executors.newFixedThreadPool(1);
    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SearchActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityTvboxSearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        initViewImpl();
        initData();
    }

    private void initViewImpl() {
        mBinding.mGridViewHistory.setHasFixedSize(true);
        mBinding.mGridViewHistory.setLayoutManager(new V7LinearLayoutManager(this.getActivity(), 1, false));
        historyAdapter = new SearchHistoryAdapter();
        mBinding.mGridViewHistory.setAdapter(historyAdapter);
        historyAdapter.setOnItemClickListener((adapter, view, position) -> search(historyAdapter.getItem(position)));
        historyAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            return historyAdapter.onLongClick(view, position);
        });
        historyAdapter.attach();

        mBinding.mGridViewWord.setHasFixedSize(true);
        mBinding.mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.getActivity(), 1, false));
        wordAdapter = new PinyinAdapter();
        mBinding.mGridViewWord.setAdapter(wordAdapter);
        wordAdapter.setOnItemClickListener((adapter, view, position) -> search(wordAdapter.getItem(position)));

        mBinding.mGridViewWordIQY.setHasFixedSize(true);
        mBinding.mGridViewWordIQY.setLayoutManager(new V7LinearLayoutManager(this.getActivity(), 1, false));
        wordIQYAdapter = new PinyinAdapter();
        mBinding.mGridViewWordIQY.setAdapter(wordIQYAdapter);
        wordIQYAdapter.setOnItemClickListener((adapter, view, position) -> search(wordIQYAdapter.getItem(position)));

        mBinding.tvSearch.setOnClickListener(v -> {
            FastClickCheckUtil.check(v);
            String wd = mBinding.etSearch.getText().toString().trim();
            if (!TextUtils.isEmpty(wd)) {
                search(wd);
            } else {
                Toast.makeText(getActivity(), "输入内容不能为空", Toast.LENGTH_SHORT).show();
            }
        });
        mBinding.tvClear.setOnClickListener(v -> {
            FastClickCheckUtil.check(v);
            mBinding.etSearch.setText("");
            loadRec("");
        });

        mBinding.keyBoardRoot.setOnSearchKeyListener((pos, key) -> {
            if (pos == 0) {
                RemoteDialog remoteDialog = new RemoteDialog(getActivity());
                remoteDialog.show();
            } else {
                String text = mBinding.etSearch.getText().toString().trim();
                if (pos > 1)  text += key;
                if (pos == 1 && !text.isEmpty()) text = text.substring(0, text.length() - 1);
                mBinding.etSearch.setText(text);
                loadRec(text);
            }
        });
    }

    private void loadRec(String key) {
        loadRecQQ(key);
        loadRecIQY(key);
    }

    private void initData() {
        loadRec("");
    }

    private void loadRecQQ(String key) {
        if (key == null || key.isEmpty()) {
            if(hotQQ != null) {
                App.post(() -> wordAdapter.setNewData(hotQQ));
                return;
            }
            OkHttp.newCall(String.format("https://node.video.qq.com/x/api/hot_mobilesearch?channdlId=0&_=%d", System.currentTimeMillis())).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    try {
                        ArrayList<String> hots = new ArrayList<>();
                        JsonArray itemList = JsonParser.parseString(response.body().string()).getAsJsonObject().get("data").getAsJsonObject().get("itemList").getAsJsonArray();
                        for (JsonElement ele : itemList) {
                            JsonObject obj = (JsonObject) ele;
                            hots.add(obj.get("title").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0]);
                        }
                        hotQQ = hots;
                        App.post(() -> wordAdapter.setNewData(hotQQ));
    
                    }catch (Exception e){
                        
                    }
                }
            });
        } else {
            executor.execute(()->{
                try {
                    String url = "https://pbaccess.video.qq.com/trpc.videosearch.smartboxServer.HttpRountRecall/Smartbox";
                    HashMap<String, String> header = new HashMap<>();
                    header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                    header.put("Referer", "https://v.qq.com/");
                    header.put("Content-Type", "application/json");
                    //{"query":"dl","appID":"3172","appKey":"lGhFIPeD3HsO9xEp","pageNum":0,"pageSize":10}
                    JSONObject body = new JSONObject();
                    body.put("query", key).put("pageNum", 0).put("pageSize",10).put("appID", "").put("appKey", "");
                    String res = com.github.tvbox.osc.net.OkHttp.postJson(url, body.toString(), header);
                    JSONObject result = new JSONObject(res);
                    ArrayList<String> hots = new ArrayList<>();
                    JSONArray arr =  result.getJSONObject("data").getJSONArray("smartboxItemList");
                    for (int i =0; i < arr.length(); ++i){
                        JSONObject basicDoc = arr.getJSONObject(i).getJSONObject("basicDoc");
                        String name = basicDoc.getString("title")
                                .replace("<em>", "")
                                .replace("</em>","")
                                .trim();
                        hots.add(name);
                    }
                    App.post(() -> wordAdapter.setNewData(hots));
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
    }

    private void loadRecIQY(String key) {
        if (key == null || key.isEmpty()) {
            if(hotIQY != null) {
                App.post(() -> wordIQYAdapter.setNewData(hotIQY));
                return;
            }
            OkHttp.newCall("https://v.api.aa1.cn/api/iqy-hot/").enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                try {
                    ArrayList<String> hots = new ArrayList<>();
                    String result = response.body().string();
                    result = result.substring(result.indexOf('['), result.indexOf(']') + 1);
                    JsonArray itemList = JsonParser.parseString(result).getAsJsonArray();
                    for (JsonElement ele : itemList) {
                        JsonObject obj = (JsonObject) ele;
                        hots.add(obj.get("query").getAsString().trim());
                    }
                    hotIQY = hots;
                    App.post(() -> wordIQYAdapter.setNewData(hotIQY));
                }catch (Exception e){
                }
                }
            });
        } else {
            OkHttp.newCall(String.format("https://suggest.video.iqiyi.com/?if=mobile&key=%s", key)).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                try {
                    ArrayList<String> hots = new ArrayList<>();
                    JsonArray itemList = JsonParser.parseString(response.body().string()).getAsJsonObject().get("data").getAsJsonArray();
                    for (JsonElement ele : itemList) {
                        JsonObject obj = (JsonObject) ele;
                        hots.add(obj.get("name").getAsString().trim().replaceAll("<|>|《|》|-", ""));
                    }
                    App.post(() -> wordIQYAdapter.setNewData(hots));
                }catch (Exception e){
                }
                }
            });
        }
    }

    private void search(String title) {
        historyAdapter.add(title);
        FastSearchActivity.start(getActivity(), ApiConfig.get().getHome().getKey(), title);
    }
}