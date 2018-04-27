package com.zsq.android.uikit.test.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsq.android.uikit.R;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by ZHAOSHENGQI467 on 2017/9/30.
 */

public class ContentFragment2 extends BaseFragment {

    private static final String TAG = "ContentFragment2";

    private ArrayList<BaseContentFragment> pagerItemList = new ArrayList<>();
    private PagerAdapter mAdapter;
    private ViewPager mPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_layout2, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager = view.findViewById(R.id.pager);
        PageFragment1 page1 = new PageFragment1();
        PageFragment3 page3 = new PageFragment3();
        PageFragment4 page4 = new PageFragment4();
        pagerItemList.add(page1);
        pagerItemList.add(page3);
        pagerItemList.add(page4);
        mAdapter = new MyAdapter(getFragmentManager());
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                Log.d(TAG, "position: " + position + " positionOffset: " +
//                        positionOffset + " positionOffsetPixels: " + positionOffsetPixels);
            }

            @Override
            public void onPageScrollStateChanged(int position) {

            }
        });
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin(15);
//        mPager.setOffscreenPageLimit(2);

        Observable.timer(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Long>() {
            @Override
            public void accept(@NonNull Long aLong) throws Exception {
                PageFragment2 page2 = new PageFragment2();
                pagerItemList.add(1, page2);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return pagerItemList.size();
        }

        @Override
        public Fragment getItem(int position) {
            // 如果使用集合缓存Fragment，如果集合数据发生了改变，
            // 则会发生crash："java.lang.IllegalStateException:
            // Can't change tag of fragment..."
            // 因为FragmentPagerAdapter根据页面的位置为已经加载的fragment
            // 设置了相应的tag，如果fragment的位置发生了变化，相应的tag也会发生变化，
            // 这时系统就会报修改Fragment的tag的异常
            Log.d(TAG, "getItem, position: " + position);
            Fragment fragment;
            if (position < pagerItemList.size())
                fragment = pagerItemList.get(position);
            else
                fragment = pagerItemList.get(0);
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            // 必须重写此方法，否则当集合改变时，数据不会刷新
            return Integer.parseInt(
                    ((BaseContentFragment) object).mIdentifier);
        }

        @Override
        public long getItemId(int position) {
            // FragmentPagerAdapter加载fragment时，返回每个item的tag值
            return Long.parseLong(pagerItemList.get(position).mIdentifier);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            Log.d(TAG, "destroyItem...");
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            Log.d(TAG, "setPrimaryItem...");
        }
    }
}
