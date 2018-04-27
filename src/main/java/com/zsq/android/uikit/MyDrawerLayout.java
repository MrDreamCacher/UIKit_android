package com.zsq.android.uikit;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ZHAOSHENGQI467 on 2017/11/9.
 */

public class MyDrawerLayout extends ViewGroup {

    private static final int MIN_DRAWER_MARGIN = 64;

    private static final int MIN_FLING_VELOCITY = 400;

    private int mMinDrawerMargin;

    private View mLeftMenuView;
    private View mContentView;

    private ViewDragHelper mHelper;

    private float mLeftMenuOnScreen;

    public MyDrawerLayout(Context context) {
        this(context, null);
    }

    public MyDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MyDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        mHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {

            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return child == mLeftMenuView;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return Math.max(-mLeftMenuView.getWidth(), Math.min(0, left));
            }

            @Override
            public void onEdgeDragStarted(int edgeFlags, int pointerId) {
                mHelper.captureChildView(mLeftMenuView, pointerId);
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return child == mLeftMenuView ? mLeftMenuView.getWidth() : 0;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                final int menuWidth = mLeftMenuView.getWidth();
                mLeftMenuOnScreen = (float) ((left + menuWidth) / menuWidth);
                invalidate();
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                final int childWidth = releasedChild.getWidth();
                float offset = (childWidth + releasedChild.getLeft()) * 1.0f / childWidth;
                // xvel的值只有大于我们设置的minVelocity才会出现大于0，否则一直等于0
                mHelper.settleCapturedViewAt(xvel > 0 || xvel == 0 &&
                        offset > 0.5f ? 0 : -childWidth, releasedChild.getTop());
                invalidate();
            }
        });
        mHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        mHelper.setMinVelocity(minVel);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        MarginLayoutParams lps = (MarginLayoutParams) mLeftMenuView.getLayoutParams();
        final int menuWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                lps.leftMargin + lps.rightMargin, lps.width);
        final int menuHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                lps.topMargin + lps.bottomMargin, lps.height);
        mLeftMenuView.measure(menuWidthSpec, menuHeightSpec);

        lps = (MarginLayoutParams) mContentView.getLayoutParams();
        final int contentWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                lps.leftMargin + lps.rightMargin, lps.width);
        final int contentHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                lps.topMargin + lps.bottomMargin, lps.height);
        mContentView.measure(contentWidthSpec, contentHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        MarginLayoutParams lps = (MarginLayoutParams) mContentView.getLayoutParams();
        mContentView.layout(lps.leftMargin, lps.topMargin,
                lps.leftMargin + mContentView.getMeasuredWidth(),
                lps.topMargin + mContentView.getMeasuredHeight());

        lps = (MarginLayoutParams) mLeftMenuView.getLayoutParams();
        final int menuWidth = mLeftMenuView.getMeasuredWidth();
        int childLeft = -menuWidth + (int) (menuWidth * mLeftMenuOnScreen);
        mLeftMenuView.layout(childLeft, lps.topMargin,
                childLeft + menuWidth,
                lps.topMargin + mLeftMenuView.getMeasuredHeight());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLeftMenuView = getChildAt(1);
        mContentView = getChildAt(0);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }
}
