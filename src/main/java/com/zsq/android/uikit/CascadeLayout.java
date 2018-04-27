package com.zsq.android.uikit;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;


/**
 * 自定义层叠布局view
 *
 * Created by ZHAOSHENGQI467 on 16/3/2.
 */
public class CascadeLayout extends ViewGroup {

    private int mHorizontalSpacing;

    private int mVerticalSpacing;

    public CascadeLayout(Context context) {
        super(context);
    }

    public CascadeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CascadeViewGroup);
        mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.CascadeViewGroup_horizontal_spacing,
                getResources().getDimensionPixelSize(R.dimen.default_horizontal_spacing));
        mVerticalSpacing = a.getDimensionPixelSize(R.styleable.CascadeViewGroup_vertical_spacing,
                getResources().getDimensionPixelSize(R.dimen.default_vertical_spacing));
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tWidth = 0, tHeight = 0;

        int cl = getPaddingLeft();
        int ct = getPaddingTop();
        int verticalSpacing = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            verticalSpacing = mVerticalSpacing;

            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            LayoutParams lps = (LayoutParams) child.getLayoutParams();
            cl = getPaddingLeft() + mHorizontalSpacing * i;

            lps.x = cl;
            lps.y = ct;

            if (lps.verticalSpacing >= 0) {
                verticalSpacing = lps.verticalSpacing;
            }


            ct += verticalSpacing;

            if (i == count - 1) {
                tWidth = cl + child.getMeasuredWidth() + getPaddingRight();
                tHeight = ct + child.getMeasuredHeight() + getPaddingBottom();
            }
        }

        setMeasuredDimension(resolveSize(tWidth, widthMeasureSpec),
                resolveSize(tHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), lp.y
                    + child.getMeasuredHeight());
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        int x;

        int y;

        public int verticalSpacing;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CascadeViewGroup_LayoutParams);
            verticalSpacing = a.getDimensionPixelSize(
                    R.styleable.CascadeViewGroup_LayoutParams_layout_vertical_spacing, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

}
