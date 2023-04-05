package com.github.tvbox.osc.util;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.orhanobut.hawk.Hawk;

import java.util.List;

public class DataLoader {
    public Result val;
    public Site site;
    private boolean loaded = false;  // 配置文件是否加载成功


    private static class Loader {
        static volatile DataLoader INSTANCE = new DataLoader();
    }

    public static DataLoader get() {
        return DataLoader.Loader.INSTANCE;
    }

    public DataLoader(){
    }

    public void init(){
        if(site != null) return;
        site = Hawk.get("site", new Site());
        val = Hawk.get("homeContent", null);
    }

    // 保存首页数据
    public void put(Site site, Result val){
        this.site = site;
        this.val = val;
        new Thread(()->{
            Hawk.put("site", site);
            Hawk.put("homeContent", val);
        }).start();
    }
    public boolean isSiteChanged(Site site){
        if(this.site == null || site == null) return false;
        if(!this.site.getKey().equals(site.getKey())) return true;
        if(!this.site.getName().equals(site.getName())) return true;
        return false;
    }
    public void clear(){
        Hawk.delete("homeSite");
        Hawk.delete("homeContent");
        site = new Site();
        val = null;
    }

    // 比较两个result的calss和filters是否相同
    public boolean refresh(Site site, Result rhs) {
        if(this.val == null || rhs == null) {
            if(rhs != null) this.put(site, rhs);
            return false;
        }
        List<Class> rhsTypes = rhs.getTypes();
        List<Class> lhsTypes = this.val.getTypes();
        boolean changed = isSiteChanged(site);
        this.put(site, rhs);
        if(changed || rhsTypes.size() != lhsTypes.size()) return true;
        for (int i =0; i < rhsTypes.size(); ++i){
            if(!rhsTypes.get(i).getTypeId().equals(lhsTypes.get(i).getTypeId())) return true;
        }
        //TODO: 比较Filters
        return false;
    }

    public void setLoaded(boolean val){
        loaded = val;
    }

    public boolean isLoaded(){
        return loaded;
    }

}
