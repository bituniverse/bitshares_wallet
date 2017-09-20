package com.bitshares.bitshareswallet;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;


public class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    public boolean isShowing = false;
    private ViewPager pager;
    private BtsFragmentPageAdapter pageAdapter;
    private boolean hasPager = false;
    private boolean callShowWhenResume = false;
    public void notifyUpdate() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isShowing && callShowWhenResume){
            onShow();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(isShowing){
            onHide();
        }
        callShowWhenResume = false;
    }

    public void updateShowing(boolean isShowing){
        if(this.isShowing!=isShowing){
            if(this.isShowing){
                onHide();
            } else if(getView()!=null){
                onShow();
            } else {
                callShowWhenResume = true;
            }
        }
    }

    /**
     * 处于pager的显示位置时被调用
     */
    public void onShow(){
        isShowing = true;
        callShowWhenResume = false;
        if(hasPager){
            pageAdapter.updateShowing(true);
        }
        Log.i(TAG, this +" isShow");
    }

    /**
     * 处于pager的隐藏位置时被调用
     */
    public void onHide(){
        isShowing = false;
        if(hasPager){
            pageAdapter.updateShowing(false);
        }
        callShowWhenResume = false;
        Log.i(TAG, this +" isHide");
    }


    /**
     * 设置内部Pager，设置后Fragment将自动处理Pager内部BaseFragment的onShow和onHide
     * @param pager
     * @param pageAdapter
     */
    public void initPager(ViewPager pager, BtsFragmentPageAdapter pageAdapter){
        hasPager = true;
        this.pager = pager;
        this.pageAdapter = pageAdapter;
        pager.addOnPageChangeListener(pageChangeListener);
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            pageAdapter.updatePagePosition(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
