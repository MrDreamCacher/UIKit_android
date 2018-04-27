package com.zsq.android.uikit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

/**
 * Created by ZHAOSHENGQI467 on 2017/9/28.
 */

public class RippleEffectView extends RelativeLayout {

    private static final String TAG = "RippleEffectView";

    private static final int SIMPLE_RIPPLE = 0;
    private static final int DOUBLE_RIPPLE = 1;
    private static final int RECTANGLE = 2;

    private static final int ANIMATION_DURATION = 400;

    private boolean mAnimating = false;

    private int mRippleColor;
    private int mRippleType;
    private int mRipplePadding;
    private boolean mHasZoom;
    private boolean mIsCentered;
    private int mRippleAlpha;
    private int mAniDuration;
    private Paint mPaint;
    private int mRadius;
    private float mRatio;
    private float mX;
    private float mY;

    private GestureDetector mGestureDetector;

    public RippleEffectView(Context context) {
        super(context);
    }

    public RippleEffectView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleEffectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, AttributeSet attrs) {
        if (isInEditMode()) {
            return;
        }
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleView);
        mRippleColor = typedArray.getColor(R.styleable.RippleView_rv_color,
                ContextCompat.getColor(context, R.color.rippleColor));
        mRippleType = typedArray.getInt(R.styleable.RippleView_rv_type, SIMPLE_RIPPLE);
        mHasZoom = typedArray.getBoolean(R.styleable.RippleView_rv_zoom, false);
        mIsCentered = typedArray.getBoolean(R.styleable.RippleView_rv_centered, false);
        mAniDuration = typedArray.getInt(R.styleable.RippleView_rv_rippleDuration, ANIMATION_DURATION);
        mRippleAlpha = typedArray.getInt(R.styleable.RippleView_rv_alpha, 80);
        mRipplePadding = typedArray.getInt(R.styleable.RippleView_rv_ripplePadding, 0);

        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRippleColor);
        mPaint.setAlpha(mRippleAlpha);

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }
        });
    }

    private void startZoomAnimation() {
        final ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(this,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 2.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 2.0f));
        objectAnimator.setRepeatCount(1);
        objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
        objectAnimator.setDuration(mAniDuration / 2);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.start();
    }

    private void startDrawRipple() {
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1.0f);
        valueAnimator.setDuration(mAniDuration);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRatio = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mGestureDetector.onTouchEvent(ev) && !mAnimating) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            mRadius = Math.max(width, height);
            if (mRippleType != DOUBLE_RIPPLE) {
                mRadius /= 2;
            }
            mRadius -= mRipplePadding;
            if (mIsCentered || mRippleType == SIMPLE_RIPPLE) {
                mX = width / 2;
                mY = height / 2;
            } else {
                mX = ev.getX();
                mY = ev.getY();
            }
            if (mHasZoom) {
                startZoomAnimation();
            }
            startDrawRipple();
        }
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mAnimating) {
            canvas.drawCircle(mX, mY, mRadius * mRatio, mPaint);
            if (mRippleType == DOUBLE_RIPPLE) {
                //TODO
            }
        }
    }
}
