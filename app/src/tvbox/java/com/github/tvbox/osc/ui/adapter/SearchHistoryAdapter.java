package com.github.tvbox.osc.ui.adapter;

import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Prefers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    private final List<String> mItems;
    private final Gson mGson;
    public SearchHistoryAdapter() {
        super(R.layout.item_search_word_hot, new ArrayList<>());
        this.mGson = new Gson();
        this.mItems = getItems();
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tvSearchWord, item);
    }

    private List<String> getItems() {
        if (Prefers.getKeyword().isEmpty()) return new ArrayList<>();
        return mGson.fromJson(Prefers.getKeyword(), new TypeToken<List<String>>() {}.getType());
    }

    private void checkToAdd(String item) {
        int index = mItems.indexOf(item);
        if (index >= 0) this.remove(index);
        this.add(0, item);
        if (mItems.size() > 20) this.remove(20);
    }

    public void add(String item) {
        checkToAdd(item);
        Prefers.putKeyword(mGson.toJson(mItems));
    }

    public boolean onLongClick(View v, int position) {
        this.remove(position);
        Prefers.putKeyword(mGson.toJson(mItems));
        return true;
    }

    public void attach(){
        this.setNewData(this.mItems);
    }
}