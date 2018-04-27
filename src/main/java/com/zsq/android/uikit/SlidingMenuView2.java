package com.zsq.android.uikit;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by ZHAOSHENGQI467 on 2017/7/6.
 * 通过ViewDragHelper实现侧滑菜单
 */
public class SlidingMenuView2 extends FrameLayout {

    private enum Status {
        Open, Close, Dragging
    }

    private boolean mDragEnabled;

    /**
     * 拖动使菜单状态发生改变的最小拖动速度
     */
    private static final int MIN_FLING_VELOCITY = 100;

    /**
     * Content 缩放动画的X轴锚点
     */
    private float mScaleXPivot = 0.5f;

    /**
     * Content 缩放动画的Y轴锚点
     */
    private float mScaleYPivot = 0.5f;

    private Status mStatus = Status.Close;

    private View mMenuView;

    private View mContentView;

    private ViewDragHelper mDragHelper;

    public SlidingMenuView2(@NonNull Context context) {
        this(context, null);
    }

    public SlidingMenuView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenuView2(@NonNull Context context, @Nullable AttributeSet attrs,
                            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDragEnabled = true;
        mDragHelper = ViewDragHelper.create(this, 1.0f, mDraggerCallback);
    }

    public void setDragEnabled(boolean enabled) {
        mDragEnabled = enabled;
    }

    public int getMenuWidth() {
        return mMenuView.getWidth();
    }

    private Status updateStatus(int left) {
        if (left == 0) {
            mStatus = Status.Close;
        } else if (left == getMenuWidth()) {
            mStatus = Status.Open;
        } else {
            mStatus = Status.Dragging;
        }
        return mStatus;
    }

    /**
     * 打开菜单
     */
    public void openMenu() {
        if (mStatus != Status.Open) {
            if (mDragHelper.smoothSlideViewTo(mContentView, getMenuWidth(), 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    /**
     * 关闭菜单
     */
    public void closeMenu() {
        if (mStatus != Status.Close) {
            if (mDragHelper.smoothSlideViewTo(mContentView, 0, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mDragEnabled) {
            if (mStatus == Status.Close
                    && ev.getX() > mDragHelper.getEdgeSize()
                    && canViewScrollLeft(mContentView)) {
                return false;
            }
            return mDragHelper.shouldInterceptTouchEvent(ev);
        }

        return false;
    }

    private boolean canViewScrollLeft(View view) {
        if (ViewCompat.canScrollHorizontally(view, -1)) return true;
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                if (canViewScrollLeft(((ViewGroup) view).getChildAt(i))) return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return true;
    }

    private ViewDragHelper.Callback mDraggerCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContentView;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMenuWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int menuWidth = getMenuWidth();
            if (child == mContentView) {
                return Math.max(0, Math.min(menuWidth, left));
            }
            return 0;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int menuWidth = getMenuWidth();
            if (xvel == 0) {
                if (Math.abs(mContentView.getLeft()) > menuWidth / 2.0f) {
                    openMenu();
                } else {
                    closeMenu();
                }
                return;
            }

            int finalLeft = 0;
            if (xvel > MIN_FLING_VELOCITY) {
                finalLeft = menuWidth;
            } else if (xvel < -MIN_FLING_VELOCITY) {
                finalLeft = 0;
            }
            mDragHelper.settleCapturedViewAt(finalLeft, 0);
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float percent = left * 1.0f / getMenuWidth();
            performAnimation(percent);

            updateStatus(left);
        }
    };

    private void performAnimation(float percent) {
        float scale = evaluate(percent, 1f, 0.8f);
//        ViewCompat.setPivotX(mContentView, mScaleXPivot);
//        ViewCompat.setPivotY(mContentView, mScaleYPivot);
        ViewCompat.setScaleX(mContentView, scale);
        ViewCompat.setScaleY(mContentView, scale);
        ViewCompat.setElevation(mContentView, percent * 100);

        scale = evaluate(percent, 0.3f, 1f);
        ViewCompat.setScaleX(mMenuView, scale);
        ViewCompat.setScaleY(mMenuView, scale);

        ViewCompat.setAlpha(mMenuView, evaluate(percent, 0.2f, 1.0f));
    }

    private float evaluate(float percent, float startVal, float endVal) {
        return (endVal - startVal) * percent + startVal;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalStateException("sliding view must have two children, " +
                    "the first is menu view, and the second is content view");
        }
        mMenuView = getChildAt(0);
        mContentView = getChildAt(1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mContentView.layout(0, 0,
                mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
    }
}
