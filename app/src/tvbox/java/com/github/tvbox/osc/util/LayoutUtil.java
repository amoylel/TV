package com.github.tvbox.osc.util;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.utils.ResUtil;

public class LayoutUtil {
    private int screenWidth;
    private static class Loader {
        static volatile LayoutUtil INSTANCE = new LayoutUtil();
    }

    public static LayoutUtil get() {
        return LayoutUtil.Loader.INSTANCE;
    }

    public LayoutUtil() {
        updateLayoutSize();
    }
    public void updateLayoutSize() {
        int space =  160 +  (30 * (Product.getColumn() - 1));
        screenWidth = ResUtil.getScreenWidthPx() - space;
    }

    public int getWidth(){
        return getWidth(0);
    }

    public int getHeight(){
        return getHeight(0);
    }

    public int getWidth(int reduce){
        return (screenWidth - reduce) / Product.getColumn();
    }

    public int getHeight(int reduce){
        return (int) (getWidth(reduce) / 0.75f);
    }

}
