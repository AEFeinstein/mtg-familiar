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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import com.gelakinetic.mtgfam.helpers.IndeterminateProgressBar;

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
	private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
	private static final float PROGRESS_BAR_HEIGHT = 4;
	private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
	private static final int REFRESH_TRIGGER_DISTANCE = 120;

	private IndeterminateProgressBar mProgressBar; //the thing that shows progress is going
	private View mTarget; //the content that gets pulled down
	private int mOriginalOffsetTop;
	private int mFrom;
	private boolean mRefreshing = false;
	private float mDistanceToTriggerSync = -1;
	private int mMediumAnimationDuration;
	private float mFromPercentage = 0;
	private float mCurrPercentage = 0;
	private int mProgressBarHeight;
	private int mCurrentTargetOffsetTop;
	private final DecelerateInterpolator mDecelerateInterpolator;
	private static final int[] LAYOUT_ATTRS = new int[]{
			android.R.attr.enabled
	};

	private final Animation mAnimateToStartPosition = new Animation() {
		@Override
		public void applyTransformation(float interpolatedTime, Transformation t) {
			int targetTop = 0;
			if (mFrom != mOriginalOffsetTop) {
				targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
			}
			int offset = targetTop - mTarget.getTop();
			final int currentTop = mTarget.getTop();
			if (offset + currentTop < 0) {
				offset = 0 - currentTop;
			}
			setTargetOffsetTopAndBottom(offset);
		}
	};

	private Animation mShrinkTrigger = new Animation() {
		@Override
		public void applyTransformation(float interpolatedTime, Transformation t) {
			float percent = mFromPercentage + ((0 - mFromPercentage) * interpolatedTime);
			mProgressBar.setTriggerPercentage(percent);
		}
	};

	private final AnimationListener mReturnToStartPositionListener = new BaseAnimationListener() {
		@Override
		public void onAnimationEnd(Animation animation) {
			// Once the target content has returned to its start position, reset
			// the target offset to 0
			mCurrentTargetOffsetTop = 0;
		}
	};

	private final AnimationListener mShrinkAnimationListener = new BaseAnimationListener() {
		@Override
		public void onAnimationEnd(Animation animation) {
			mCurrPercentage = 0;
		}
	};

	private final Runnable mReturnToStartPosition = new Runnable() {

		@Override
		public void run() {
			animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(),
					mReturnToStartPositionListener);
		}

	};

	// Cancel the refresh gesture and animate everything back to its original state.
	private final Runnable mCancel = new Runnable() {

		@Override
		public void run() {
			// Timeout fired since the user last moved their finger; animate the
			// trigger to 0 and put the target back at its original position
			if (mProgressBar != null) {
				mFromPercentage = mCurrPercentage;
				mShrinkTrigger.setDuration(mMediumAnimationDuration);
				mShrinkTrigger.setAnimationListener(mShrinkAnimationListener);
				mShrinkTrigger.reset();
				mShrinkTrigger.setInterpolator(mDecelerateInterpolator);
				startAnimation(mShrinkTrigger);
			}
			animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(),
					mReturnToStartPositionListener);
		}

	};

	/**
	 * Simple constructor to use when creating a IndeterminateRefreshLayout from code.
	 *
	 * @param context A Context to pass to Super
	 */
//	public IndeterminateRefreshLayout(Context context) {
//		this(context, null);
//	}

	/**
	 * Constructor that is called when inflating IndeterminateRefreshLayout from XML.
	 *
	 * @param context A Context to pass to Super
	 * @param attrs   An AttributeSet to pass to Super
	 */
	public IndeterminateRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		assert getResources() != null;
		mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

		setWillNotDraw(false);
		mProgressBar = new IndeterminateProgressBar(this);
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);
		mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

		final TypedArray typedArray = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
		assert typedArray != null;
		setEnabled(typedArray.getBoolean(0, true));
		typedArray.recycle();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		removeCallbacks(mCancel);
		removeCallbacks(mReturnToStartPosition);
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks(mReturnToStartPosition);
		removeCallbacks(mCancel);
	}

	private void animateOffsetToStartPosition(int from, AnimationListener listener) {
		mFrom = from;
		mAnimateToStartPosition.reset();
		mAnimateToStartPosition.setDuration(mMediumAnimationDuration);
		mAnimateToStartPosition.setAnimationListener(listener);
		mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
		mTarget.startAnimation(mAnimateToStartPosition);
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
			mCurrPercentage = 0;
			mRefreshing = refreshing;
			if (mRefreshing) {
				mProgressBar.start();
			}
			else {
				mProgressBar.stop();
			}
		}
	}

	/**
	 * Set the four colors used in the progress animation. The first color will
	 * also be the color of the bar that grows in response to a user swipe
	 * gesture.
	 *
	 * @param colorRes1 Color resource.
	 * @param colorRes2 Color resource.
	 * @param colorRes3 Color resource.
	 * @param colorRes4 Color resource.
	 */
	public void setColorScheme(int colorRes1, int colorRes2, int colorRes3, int colorRes4) {
		ensureTarget();
		final Resources res = getResources();
		assert res != null;
		final int color1 = res.getColor(colorRes1);
		final int color2 = res.getColor(colorRes2);
		final int color3 = res.getColor(colorRes3);
		final int color4 = res.getColor(colorRes4);
		mProgressBar.setColorScheme(color1, color2, color3, color4);
	}

	private void ensureTarget() {
		// Don't bother getting the parent height if the parent hasn't been laid out yet.
		if (mTarget == null) {
			if (getChildCount() > 1 && !isInEditMode()) {
				throw new IllegalStateException(
						"IndeterminateRefreshLayout can host only one direct child");
			}
			mTarget = getChildAt(0);
			if (mTarget != null) {
				mOriginalOffsetTop = mTarget.getTop() + getPaddingTop();
			}
		}
		if (mDistanceToTriggerSync == -1) {
			if (getParent() != null && ((View) getParent()).getHeight() > 0) {
				assert getResources() != null;
				final DisplayMetrics metrics = getResources().getDisplayMetrics();
				mDistanceToTriggerSync = (int) Math.min(
						((View) getParent()).getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
						REFRESH_TRIGGER_DISTANCE * metrics.density);
			}
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
		mProgressBar.setBounds(0, 0, width, mProgressBarHeight);
		if (getChildCount() == 0) {
			return;
		}
		final View child = getChildAt(0);
		final int childLeft = getPaddingLeft();
		final int childTop = mCurrentTargetOffsetTop + getPaddingTop();
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

	private void setTargetOffsetTopAndBottom(int offset) {
		mTarget.offsetTopAndBottom(offset);
		mCurrentTargetOffsetTop = mTarget.getTop();
	}

	/**
	 * Simple AnimationListener to avoid having to implement unneeded methods in
	 * AnimationListeners.
	 */
	private class BaseAnimationListener implements AnimationListener {
		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
	}
}