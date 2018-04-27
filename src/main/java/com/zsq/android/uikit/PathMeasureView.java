package com.zsq.android.uikit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by ZHAOSHENGQI467 on 2017/10/2.
 */

public class PathMeasureView extends View {

    private static final String TAG = "PathMeasureView";

    private Path mCirclePath;
    private Path mDstPath;
    private float mLength;
    private PathMeasure mPathMeasure;
    private ValueAnimator mValueAnimator;
    private float mAnimatedValue;
    private Paint mPaint;

    public PathMeasureView(Context context) {
        this(context, null, 0);
    }

    public PathMeasureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathMeasureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);
        mPaint.setAntiAlias(true);

        mCirclePath = new Path();
        mCirclePath.addCircle(0, 0, 100, Path.Direction.CW);
        mDstPath = new Path();
        mPathMeasure = new PathMeasure();
        mPathMeasure.setPath(mCirclePath, true);
        mLength = mPathMeasure.getLength();

        mValueAnimator = ValueAnimator.ofFloat(0, 1.0f);
        mValueAnimator.setDuration(2000);
        mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatedValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mValueAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);
        final float x = event.getX(actionIndex);
        final float y = event.getY(actionIndex);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "action down event, x: " + x + " y: " + y +
                        " pointerId: " + event.getPointerId(0));
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                Log.d(TAG, "action pointer down event, x: " + x + " y: " + y +
                        " pointerId: " + event.getPointerId(actionIndex));
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "action move event, x: " + x + " y: " + y +
                        " pointerId: " + event.getPointerId(actionIndex));
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                Log.d(TAG, "action pointer up event, x: " + x + " y: " + y +
                        " pointerId: " + event.getPointerId(actionIndex));
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "action up event, x: " + x + " y: " + y +
                        " pointerId: " + event.getPointerId(actionIndex));
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mDstPath.reset();
        mDstPath.lineTo(0, 0);
        float startD = 0;
        float stopD = mAnimatedValue * mLength;
        mPathMeasure.getSegment(startD, stopD, mDstPath, true);

        canvas.save();
        canvas.translate(300, 300);
        canvas.drawPath(mDstPath, mPaint);
        canvas.restore();

        startD = (float) (stopD - ((0.5 - Math.abs(mAnimatedValue - 0.5)) * mLength));
        mPathMeasure.getSegment(startD, stopD, mDstPath, true);
        canvas.save();
        canvas.translate(600, 600);
        canvas.drawPath(mDstPath, mPaint);
        canvas.restore();
    }
}
