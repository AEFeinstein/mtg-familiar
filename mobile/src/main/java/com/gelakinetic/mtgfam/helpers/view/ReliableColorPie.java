package com.gelakinetic.mtgfam.helpers.view;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.highlight.PieHighlighter;
import com.github.mikephil.charting.renderer.PieChartRenderer;

public class ReliableColorPie extends PieChart {

    public ReliableColorPie(Context context) {
        super(context);
    }

    public ReliableColorPie(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReliableColorPie(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new ReliableColorPieRenderer(this, mAnimator, mViewPortHandler);
        mXAxis = null;

        mHighlighter = new PieHighlighter(this);
    }
}
