package com.github.tvbox.osc.bean;

import com.fongmi.android.tv.bean.Filter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SortData {
    public String id;
    public String name;
    public boolean select = false;
    public List<Filter> filters = new ArrayList<>();
    public HashMap<String, String> filterSelect = new HashMap<>();
    public String flag;     // 类型

    public SortData() { }
    public SortData(String id, String name, String flag) {
        this.id = id;
        this.name = name;
        this.flag = flag;
    }
}
