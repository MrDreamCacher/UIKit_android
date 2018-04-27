package com.zsq.android.uikit.test.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsq.android.uikit.R;

import java.util.ArrayList;


public class ContentFragment1 extends Fragment {

    private static final String TAG = "ContentFragment1";

    private ViewPager mPager;
    private ArrayList<Fragment> pagerItemList = new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_layout1, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager = view.findViewById(R.id.pager);
        PageFragment1 page1 = new PageFragment1();
        PageFragment2 page2 = new PageFragment2();
        PageFragment3 page3 = new PageFragment3();
        PageFragment4 page4 = new PageFragment4();
        pagerItemList.add(page1);
        pagerItemList.add(page2);
        pagerItemList.add(page3);
        pagerItemList.add(page4);
//        mAdapter = new MyAdapter(getFragmentManager());
        PagerAdapter adapter = new MyStateAdapter(getFragmentManager());
        mPager.setAdapter(adapter);
        mPager.setPageMargin(15);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (myPageChangeListener != null) {
                    myPageChangeListener.onPageSelected(position);
                }
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
    }

    public boolean isFirst() {
        return mPager.getCurrentItem() == 0;
    }

    public boolean isEnd() {
        return mPager.getCurrentItem() == pagerItemList.size() - 1;
    }

    private class MyStateAdapter extends FragmentStatePagerAdapter {

        MyStateAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            if (position < pagerItemList.size())
                fragment = pagerItemList.get(position);
            else
                fragment = pagerItemList.get(0);
            return fragment;
        }

        @Override
        public int getCount() {
            return pagerItemList.size();
        }
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
            Fragment fragment = null;
            if (position < pagerItemList.size())
                fragment = pagerItemList.get(position);
            else
                fragment = pagerItemList.get(0);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            Log.d(TAG, "destroyItem...");
        }
    }

    private MyPageChangeListener myPageChangeListener;

    public void setMyPageChangeListener(MyPageChangeListener l) {
        myPageChangeListener = l;
    }

    public interface MyPageChangeListener {

        void onPageSelected(int position);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu...");
    }
}
