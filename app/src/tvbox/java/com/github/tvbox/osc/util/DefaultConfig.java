package com.github.tvbox.osc.util;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.github.tvbox.osc.bean.SortData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class DefaultConfig {

    public static List<SortData> adjustSort(String sourceKey, List<SortData> list, boolean withMy) {
        List<SortData> data = new ArrayList<>();
        if (sourceKey != null && !sourceKey.isEmpty()) {
            Site sb = ApiConfig.get().getSite(sourceKey);
            List<String> categories = sb.getCategories();
            if (!categories.isEmpty()) {
                for (String cate : categories) {
                    for (SortData sortData : list) {
                        if (sortData.name.equals(cate)) {
                            if (sortData.filters == null)
                                sortData.filters = new ArrayList<>();
                            data.add(sortData);
                        }
                    }
                }
            } else {
                for (SortData sortData : list) {
                    if (sortData.filters == null)
                        sortData.filters = new ArrayList<>();
                    data.add(sortData);
                }
            }
        }
        data.add(0, new SortData("my0", "TVBox", "0"));
        return data;
    }
}