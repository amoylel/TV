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

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;
import okhttp3.Call;

public class SearchActivity extends BaseActivity {
    private SearchHistoryAdapter historyAdapter;
    private PinyinAdapter wordAdapter;
    private PinyinAdapter wordIQYAdapter;
    private ActivityTvboxSearchBinding mBinding;

    private static ArrayList<String> hotQQ;
    private static ArrayList<String> hotIQY;
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
            OkHttp.newCall(String.format("https://s.video.qq.com/smartbox?plat=2&ver=0&num=20&otype=json&query=%s", key)).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                try {
                    ArrayList<String> hots = new ArrayList<>();
                    String result = response.body().string();
                    JsonObject json = JsonParser.parseString(result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1)).getAsJsonObject();
                    JsonArray itemList = json.get("item").getAsJsonArray();
                    for (JsonElement ele : itemList) {
                        JsonObject obj = (JsonObject) ele;
                        hots.add(obj.get("word").getAsString().trim());
                    }
                    App.post(() -> wordAdapter.setNewData(hots));
                }catch (Exception e){
                }
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