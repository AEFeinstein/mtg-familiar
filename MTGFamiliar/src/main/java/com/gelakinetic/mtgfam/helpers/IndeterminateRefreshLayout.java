/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;


/**
 * The IndeterminateRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The IndeterminateRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 * <p/>
 * <p> This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The IndeterminateRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.</p>
 */
public class IndeterminateRefreshLayout extends ViewGroup {
	private static final float PROGRESS_BAR_HEIGHT = 4;

	private IndeterminateProgressBar mProgressBar; //the thing that shows progress is going
	private View mTarget; //the content that gets pulled down
	public boolean mRefreshing = false;
	private int mProgressBarHeight;
	private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};

	/**
	 * Constructor that is called when inflating IndeterminateRefreshLayout from XML.
	 *
	 * @param context A Context to pass to Super
	 * @param attrs   An AttributeSet to pass to Super
	 */
	public IndeterminateRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		assert getResources() != null;

		setWillNotDraw(false);
		mProgressBar = new IndeterminateProgressBar(this);
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);

		final TypedArray typedArray = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
		assert typedArray != null;
		setEnabled(typedArray.getBoolean(0, true));
		typedArray.recycle();

		/*
		 * Set the four colors used in the progress animation. The first color will
		 * also be the color of the bar that grows in response to a user swipe
		 * gesture.
		 */
		final Resources res = getResources();
		mProgressBar.setColorScheme(
				res.getColor(com.gelakinetic.mtgfam.R.color.common),
				res.getColor(com.gelakinetic.mtgfam.R.color.uncommon),
				res.getColor(com.gelakinetic.mtgfam.R.color.rare),
				res.getColor(com.gelakinetic.mtgfam.R.color.mythic));
	}

	/**
	 * Notify the widget that refresh state has changed. Do not call this when
	 * refresh is triggered by a swipe gesture.
	 *
	 * @param refreshing Whether or not the view should show refresh progress.
	 */
	public void setRefreshing(boolean refreshing) {
		if (mRefreshing != refreshing) {
			ensureTarget();
			mRefreshing = refreshing;
			if (mRefreshing) {
				mProgressBar.start();
			}
			else {
				mProgressBar.stop();
			}
		}
	}

	private void ensureTarget() {
		// Don't bother getting the parent height if the parent hasn't been laid out yet.
		if (mTarget == null) {
			if (getChildCount() > 1 && !isInEditMode()) {
				throw new IllegalStateException(
						"IndeterminateRefreshLayout can host only one direct child");
			}
			mTarget = getChildAt(0);
		}
	}

	@Override
	public void draw(@NotNull Canvas canvas) {
		super.draw(canvas);
		mProgressBar.draw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int width = getMeasuredWidth();
		final int height = getMeasuredHeight();
		mProgressBar.setBounds(width, mProgressBarHeight);
		if (getChildCount() == 0) {
			return;
		}
		final View child = getChildAt(0);
		final int childLeft = getPaddingLeft();
		final int childTop = getPaddingTop();
		final int childWidth = width - getPaddingLeft() - getPaddingRight();
		final int childHeight = height - getPaddingTop() - getPaddingBottom();
		assert child != null;
		child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (getChildCount() > 1 && !isInEditMode()) {
			throw new IllegalStateException("IndeterminateRefreshLayout can host only one direct child");
		}
		if (getChildCount() > 0) {
			int width = MeasureSpec.makeMeasureSpec(
					getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
					MeasureSpec.EXACTLY);
			int height = MeasureSpec.makeMeasureSpec(
					getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
					MeasureSpec.EXACTLY);
			View child = getChildAt(0);
			assert child != null;
			child.measure(width, height);
		}
	}
}