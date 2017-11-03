package com.bitshares.bitshareswallet;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class NonScrollViewPager extends ViewPager {
    public NonScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollViewPager(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        return false;
    }
}
