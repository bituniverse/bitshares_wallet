package com.bitshares.bitshareswallet;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;

import java.util.ArrayList;
import java.util.List;

public class BtsFragmentPageAdapter extends FragmentPagerAdapter {
    private List<Fragment> mListFragment = new ArrayList<>();
    private final List<String> mListFragmentTitle = new ArrayList<>();
    private int position = 0;
    public BtsFragmentPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mListFragmentTitle.get(position);
    }

    public void addFragment(Fragment fragment, String strTitle) {
        mListFragment.add(fragment);
        mListFragmentTitle.add(strTitle);
        this.notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return mListFragment.get(position);
    }

    @Override
    public int getCount() {
        return mListFragment.size();
    }

    @Override
    public long getItemId(int position) {
        return mListFragment.get(position).hashCode();
    }

    /**
     * 更新正在现在的Fragment的位置
     * @param position
     */
    public void updatePagePosition(int position){
        this.position = position;
        for(int i=0; i<mListFragment.size(); i++){
            Fragment fragment = mListFragment.get(i);
            if(fragment instanceof BaseFragment){
                BaseFragment baseFragment = (BaseFragment) fragment;
                if(i==position){
                    baseFragment.updateShowing(true);
                } else {
                    baseFragment.updateShowing(false);
                }
            }
        }
    }

    /**
     * 更新显示状态
     * @param isShowing
     */
    public void updateShowing(boolean isShowing){
        Fragment currFragment = mListFragment.get(position);
        if(currFragment instanceof BaseFragment){
            BaseFragment currBaseFragment = (BaseFragment) currFragment;
            currBaseFragment.updateShowing(isShowing);
        }
    }
    /**
     * 通知所有Fragment更新
     */
    public void notifyUpdate() {
        for (Fragment fragment : mListFragment) {
            if (fragment instanceof BaseFragment) {
                ((BaseFragment)fragment).notifyUpdate();
            }
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        super.destroyItem(container, position, object);
    }
}
