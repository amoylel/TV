package com.github.tvbox.osc.util;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.orhanobut.hawk.Hawk;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import okhttp3.Call;

public class DouBan {
    public interface Callback2 {
        public void onLoad(ArrayList<Vod> movies);
    }

    ArrayList<Vod> hotMovies;
    private Callback2 callback;

    private static class Loader {
        static volatile DouBan INSTANCE = new DouBan();
    }

    public static DouBan get() {
        return DouBan.Loader.INSTANCE;
    }

    synchronized public ArrayList<Vod> load(Callback2 cb){
        readHotFromCache();
        this.callback = cb;
        if(hotMovies != null && !hotMovies.isEmpty()) return hotMovies;
        new Thread(() ->  initHomeHotVod() ).start();
        return null;
    }

    synchronized private void postData(ArrayList<Vod> movies){
        if(movies == null) return;
        App.post(()->{
            if(callback != null) callback.onLoad(movies);
        });
    }

    private ArrayList<Vod> loadHots(String json) {
        ArrayList<Vod> result = new ArrayList<>();
        try {
            JsonObject infoJson = new Gson().fromJson(json, JsonObject.class);
            JsonArray array = infoJson.getAsJsonArray("data");
            for (JsonElement ele : array) {
                JsonObject obj = (JsonObject) ele;
                JSONObject tmp = new JSONObject();
                tmp.put("vod_name", obj.get("title").getAsString());
                tmp.put("vod_pic", obj.get("cover").getAsString());
                tmp.put("vod_remarks", obj.get("rate").getAsString());
                result.add(new Gson().fromJson(tmp.toString(), Vod.class));
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return result;
    }

    private void readHotFromCache() {
        if(hotMovies != null) return;
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DATE);
        String today = String.format("%d%d%d", year, month, day);
        String requestDay = Hawk.get("home_hot_day", "");
        if (requestDay.equals(today)) {
            String json = Hawk.get("home_hot", "");
            if (!json.isEmpty()) hotMovies = loadHots(json);
        }
    }

    private void initHomeHotVod() {
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            String today = String.format("%d%d%d", year, month, day);
            String doubanUrl = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=" + year + "," + year;
            OkHttp.newCall(doubanUrl).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    String netJson = response.body().string();
                    Hawk.put("home_hot_day", today);
                    Hawk.put("home_hot", netJson);
                    hotMovies = loadHots(netJson);
                    postData(hotMovies);
                }

            });
        } catch (Throwable th) {
            th.printStackTrace();
            postData(new ArrayList<>());
        }
    }
}
