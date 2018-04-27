package com.zsq.android.uikit;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.Scroller;


/**
 * Created by ZHAOSHENGQI467 on 16/3/25.
 */
public class SlidingMenuView extends RelativeLayout {

    private static final String TAG = "SlidingMenuView";

    private static final int VELOCITY = 50;

    private View mSlidingView;
    private View mMenuView;
    private View mDetailView;

    private RelativeLayout bgShade;

    private int mScreenWidth;
    private int mScreenHeight;

    private Context mContext;

    private Scroller mScroller;

    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;

    private float mLastMotionX;
    private float mLastMotionY;

    private boolean mIsBeingDragged = true;

    private boolean mCanSlideLeft = true;
    private boolean mCanSlideRight = false;

    private boolean tCanSlideLeft = true;
    private boolean tCanSlideRight = false;

    private boolean mHasClickLeft;
    private boolean mHasClickRight;

    public SlidingMenuView(Context context) {
        super(context);
    }

    public SlidingMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;

        bgShade = new RelativeLayout(context);
        LayoutParams lps = new LayoutParams(mScreenWidth, mScreenHeight);
        lps.addRule(RelativeLayout.CENTER_IN_PARENT);
        bgShade.setLayoutParams(lps);
    }

    public void addViews(View left, View center, View right) {
        setLeftView(left);
        setRightView(right);
        setCenterView(center);
    }

    public void setLeftView(View view) {
        LayoutParams behindParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        addView(view, behindParams);
        mMenuView = view;
    }

    public void setRightView(View view) {
        LayoutParams behindParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        behindParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        addView(view, behindParams);
        mDetailView = view;
    }

    public void setCenterView(View view) {
        LayoutParams aboveParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        LayoutParams bgParams = new LayoutParams(mScreenWidth, mScreenHeight);
        bgParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        View bgShadeContent = new View(mContext);
        bgShadeContent.setBackgroundResource(R.drawable.shade_bg);
        bgShade.addView(bgShadeContent, bgParams);

        addView(bgShade, bgParams);

        addView(view, aboveParams);
        mSlidingView = view;
        mSlidingView.bringToFront();
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = mSlidingView.getScrollX();
            int oldY = mSlidingView.getScrollY();

            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                // 只是mSlidingView的内容在滚动，因为mSlidingView的背景是透明的，
                // 所以可以看到菜单View
                mSlidingView.scrollTo(x, y);
                if (x < 0) {
                    bgShade.scrollTo(x + 20, y);
                } else {
                    bgShade.scrollTo(x - 20, y);
                }
            }

            invalidate();
        }
    }

    public void setCanSliding(boolean left, boolean right) {
        mCanSlideLeft = left;
        mCanSlideRight = right;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int a = ev.getAction() & MotionEventCompat.ACTION_MASK;
        Log.d(TAG, "onTouchEvent: " + a);
        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                mLastMotionY = y;
                mIsBeingDragged = false;

                if (mCanSlideLeft) {
                    mMenuView.setVisibility(View.VISIBLE);
                    mDetailView.setVisibility(View.INVISIBLE);
                }

                if (mCanSlideRight) {
                    mMenuView.setVisibility(View.INVISIBLE);
                    mDetailView.setVisibility(View.VISIBLE);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float dx = x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float yDiff = Math.abs(y - mLastMotionY);
                if (xDiff > mTouchSlop && xDiff > yDiff) {
                    if (mCanSlideLeft) {
                        float oldScrollX = mSlidingView.getScrollX();
                        if (oldScrollX < 0) {
                            mIsBeingDragged = true;
                            mLastMotionX = x;
                        } else {
                            if (dx > 0) {
                                mIsBeingDragged = true;
                                mLastMotionX = x;
                            }
                        }
                    } else if (mCanSlideRight) {
                        float oldScrollX = mSlidingView.getScrollX();
                        if (oldScrollX > 0) {
                            mIsBeingDragged = true;
                            mLastMotionX = x;
                        } else {
                            if (dx < 0) {
                                mIsBeingDragged = true;
                                mLastMotionX = x;
                            }
                        }
                    }
                }
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int a = event.getAction() & MotionEventCompat.ACTION_MASK;
        Log.d(TAG, "onTouchEvent: " + a);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);

        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                mLastMotionX = x;
                mLastMotionY = y;

                // 当左边抽屉拉开时不处理触摸事件
                if (mSlidingView.getScrollX() == -getMenuViewWidth()
                        && mLastMotionX < getMenuViewWidth()) {
                    return false;
                }

                //当右侧抽屉拉开时不处理触摸事件
                if (mSlidingView.getScrollX() == getDetailViewWidth()
                        && mLastMotionX > getMenuViewWidth()) {
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged) {
                    final float deltaX = mLastMotionX - x;
                    mLastMotionX = x;
                    float oldScrollX = mSlidingView.getScrollX();
                    float scrollX = oldScrollX + deltaX;
                    if (mCanSlideLeft && scrollX > 0) {
                        scrollX = 0;
                    }

                    if (mCanSlideRight && scrollX < 0) {
                        scrollX = 0;
                    }

                    if (deltaX < 0 && oldScrollX < 0) {
                        final float leftBound = 0;
                        final float rightBound = -getMenuViewWidth();
                        if (scrollX > leftBound) {
                            scrollX = leftBound;
                        } else if (scrollX < rightBound) {
                            scrollX = rightBound;
                        }
                    } else if (deltaX > 0 && oldScrollX > 0) {
                        final float rightBound = getDetailViewWidth();
                        final float leftBound = 0;
                        if (scrollX < leftBound) {
                            scrollX = leftBound;
                        } else if (scrollX > rightBound) {
                            scrollX = rightBound;
                        }
                    }

                    mSlidingView.scrollTo((int) scrollX,
                            mSlidingView.getScrollY());
                    if (scrollX < 0) {
                        bgShade.scrollTo((int) scrollX + 20,
                                mSlidingView.getScrollY());
                    } else {
                        bgShade.scrollTo((int) scrollX - 20,
                                mSlidingView.getScrollY());
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(100);

                    //从左向右划返回正数，从右向左划返回负数
                    float xVelocity = velocityTracker.getXVelocity();// 滑动的速度
                    int oldScrollX = mSlidingView.getScrollX();
                    int dx = 0;
                    if (oldScrollX <= 0 && mCanSlideLeft) {// left view
                        if (xVelocity > VELOCITY) {
                            dx = -getMenuViewWidth() - oldScrollX;
                        } else if (xVelocity < -VELOCITY) {
                            dx = -oldScrollX;
                            if (mHasClickLeft) {
                                mHasClickLeft = false;
                                setCanSliding(tCanSlideLeft, tCanSlideRight);
                            }
                        } else if (oldScrollX < -getMenuViewWidth() / 2) {
                            dx = -getMenuViewWidth() - oldScrollX;
                        } else if (oldScrollX >= -getMenuViewWidth() / 2) {
                            dx = -oldScrollX;
                            if (mHasClickLeft) {
                                mHasClickLeft = false;
                                setCanSliding(tCanSlideLeft, tCanSlideRight);
                            }
                        }
                    }

                    if (oldScrollX >= 0 && mCanSlideRight) {
                        if (xVelocity < -VELOCITY) {
                            dx = getDetailViewWidth() - oldScrollX;
                        } else if (xVelocity > VELOCITY) {
                            dx = -oldScrollX;
                            if (mHasClickRight) {
                                mHasClickRight = false;
                                setCanSliding(tCanSlideLeft, tCanSlideRight);
                            }
                        } else if (oldScrollX > getDetailViewWidth() / 2) {
                            dx = getDetailViewWidth() - oldScrollX;
                        } else if (oldScrollX <= getDetailViewWidth() / 2) {
                            dx = -oldScrollX;
                            if (mHasClickRight) {
                                mHasClickRight = false;
                                setCanSliding(tCanSlideLeft, tCanSlideRight);
                            }
                        }
                    }

                    smoothScrollTo(dx);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private int getMenuViewWidth() {
        if (mMenuView == null) {
            return 0;
        }
        return mMenuView.getWidth();
    }

    private int getDetailViewWidth() {
        if (mDetailView == null) {
            return 0;
        }
        return mDetailView.getWidth();
    }

    void smoothScrollTo(int dx) {
        int duration = 500;
        mScroller.startScroll(mSlidingView.getScrollX(), mSlidingView.getScrollY(), dx,
                mSlidingView.getScrollY(), duration);
        invalidate();
    }

    /*
	 * 显示左侧边的view
	 * */
    public void showLeftView() {
        int menuWidth = mMenuView.getWidth();
        int oldScrollX = mSlidingView.getScrollX();
        if (oldScrollX == 0) {
            mMenuView.setVisibility(View.VISIBLE);
            mDetailView.setVisibility(View.INVISIBLE);
            smoothScrollTo(-menuWidth);
            tCanSlideLeft = mCanSlideLeft;
            tCanSlideRight = mCanSlideRight;
            mHasClickLeft = true;
            setCanSliding(true, false);
        } else if (oldScrollX == -menuWidth) {
            smoothScrollTo(menuWidth);
            if (mHasClickLeft) {
                mHasClickLeft = false;
                setCanSliding(tCanSlideLeft, tCanSlideRight);
            }
        }
    }

    /*显示右侧边的view*/
    public void showRightView() {
        int menuWidth = mDetailView.getWidth();
        int oldScrollX = mSlidingView.getScrollX();
        if (oldScrollX == 0) {
            mMenuView.setVisibility(View.INVISIBLE);
            mDetailView.setVisibility(View.VISIBLE);
            smoothScrollTo(menuWidth);
            tCanSlideLeft = mCanSlideLeft;
            tCanSlideRight = mCanSlideRight;
            mHasClickRight = true;
            setCanSliding(false, true);
        } else if (oldScrollX == menuWidth) {
            smoothScrollTo(-menuWidth);
            if (mHasClickRight) {
                mHasClickRight = false;
                setCanSliding(tCanSlideLeft, tCanSlideRight);
            }
        }
    }

}
