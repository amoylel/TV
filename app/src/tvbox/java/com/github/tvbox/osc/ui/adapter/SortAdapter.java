package com.github.tvbox.osc.ui.adapter;



import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.fongmi.android.tv.R;
import com.github.tvbox.osc.bean.SortData;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class SortAdapter extends BaseQuickAdapter<SortData, BaseViewHolder> {
    public SortAdapter() {
        super(R.layout.item_home_sort, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, SortData item) {
        helper.setText(R.id.tvTitle, item.name);
    }
    public SortData findItemByName(String name){
       for(SortData item: this.getData()){
           if(item.name.equals(name)) return item;
       }
       return new SortData();
    }
}