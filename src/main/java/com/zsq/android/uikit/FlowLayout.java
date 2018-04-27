package com.zsq.android.uikit;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZHAOSHENGQI467 on 16/3/1.
 * 瀑布流布局
 */
public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private List<List<View>> mAllViews = new ArrayList<>();

    private List<Integer> mLineHeight = new ArrayList<>();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 此方法可以优化,在onMeasure方法存储坐标值,onLayout方法直接使用
        mAllViews.clear();
        mLineHeight.clear();

        int pWidth = getMeasuredWidth();

        int lineWidth = getPaddingLeft();
        int lineHeight = getPaddingTop();

        List<View> lineViews = new ArrayList<>();
        int cCount = getChildCount();

        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);

            MarginLayoutParams lps = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (childWidth + lps.leftMargin + lps.rightMargin + lineWidth > pWidth) {
                mLineHeight.add(lineHeight);

                mAllViews.add(lineViews);

                lineWidth = 0;

                lineHeight = 0;

                lineViews = new ArrayList<>();
            }

            lineWidth += childWidth + lps.leftMargin + lps.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lps.topMargin + lps.bottomMargin);
            lineViews.add(child);
        }

        mLineHeight.add(lineHeight);
        mAllViews.add(lineViews);

        int left = 0;
        int top = 0;
        int lineNum = mAllViews.size();
        for (int i = 0; i < lineNum; i++) {
            lineViews = mAllViews.get(i);

            lineHeight = mLineHeight.get(i);

            int lineViewNum = lineViews.size();
            for (int j = 0; j < lineViewNum; j++) {
                View child = lineViews.get(j);

                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                MarginLayoutParams lps = (MarginLayoutParams) child.getLayoutParams();

                int lc = left + lps.leftMargin;
                int tc = top + lps.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();

                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lps.rightMargin
                        + lps.leftMargin;
            }

            left = getPaddingLeft();
            top += lineHeight;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int pWidth = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int tWidth = 0;
        int tHeight = 0;

        int lineWidth = 0;
        int lineHeight = 0;

        int cCount = getChildCount();

        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            Log.d("FlowLayout", "child width: " + child.getMeasuredWidth());

            MarginLayoutParams lps = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth() + lps.leftMargin + lps.rightMargin;
            int childHeight = child.getMeasuredHeight() + lps.topMargin + lps.bottomMargin;

            if (lineWidth + childWidth > pWidth) {
                tWidth = Math.max(lineWidth, childWidth);

                lineWidth = childWidth;

                tHeight += lineHeight;

                lineHeight = childHeight;
            } else {
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }

            if (i == cCount - 1) {
                tWidth = Math.max(tWidth, lineWidth);
                tHeight += lineHeight;
            }
        }

        tWidth += getPaddingLeft() + getPaddingRight();
        tHeight += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension((widthMode == MeasureSpec.EXACTLY) ? pWidth
                : tWidth, (heightMode == MeasureSpec.EXACTLY) ? pWidth
                : tHeight);
    }

}
