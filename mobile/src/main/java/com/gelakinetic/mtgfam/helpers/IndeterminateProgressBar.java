package com.gelakinetic.mtgfam.helpers;

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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;


/**
 * Custom progress bar that shows a cycle of colors as widening circles that
 * overdraw each other. When finished, the bar is cleared from the inside out as
 * the main cycle continues. Before running, this can also indicate how close
 * the user is to triggering something (e.g. how far they need to pull down to
 * trigger a refresh).
 */
public final class IndeterminateProgressBar {

    // Default progress animation colors are grays.
    private final static int COLOR1 = 0xFFFF0000;
    private final static int COLOR2 = 0xFF00FF00;
    private final static int COLOR3 = 0xFF0000FF;
    private final static int COLOR4 = 0xFF808080;

    // The duration of the animation cycle.
    private static final int ANIMATION_DURATION_MS = 2000;

    // The duration of the animation to clear the bar.
    private static final int FINISH_ANIMATION_DURATION_MS = 1000;

    // Interpolator for varying the speed of the animation.
    private static final Interpolator INTERPOLATOR = BakedBezierInterpolator.getInstance();

    private final Paint mPaint = new Paint();
    private final RectF mClipRect = new RectF();
    private final View mParent;
    private final Rect mBounds = new Rect();
    private long mStartTime;
    private long mFinishTime;
    private boolean mRunning;
    // Colors used when rendering the animation,
    private int mColor1;
    private int mColor2;
    private int mColor3;
    private int mColor4;

    public IndeterminateProgressBar(View parent) {
        mParent = parent;
        mColor1 = COLOR1;
        mColor2 = COLOR2;
        mColor3 = COLOR3;
        mColor4 = COLOR4;
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     *
     * @param color1 Integer representation of a color.
     * @param color2 Integer representation of a color.
     * @param color3 Integer representation of a color.
     * @param color4 Integer representation of a color.
     */
    public void setColorScheme(int color1, int color2, int color3, int color4) {
        mColor1 = color1;
        mColor2 = color2;
        mColor3 = color3;
        mColor4 = color4;
    }

    /**
     * Start showing the progress animation.
     */
    public void start() {
        if (!mRunning) {
            mStartTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = true;
            mParent.postInvalidate();
        }
    }

    /**
     * Stop showing the progress animation.
     */
    public void stop() {
        if (mRunning) {
            mFinishTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = false;
            mParent.postInvalidate();
        }
    }

    public void draw(Canvas canvas) {
        final int width = mBounds.width();
        final int height = mBounds.height();
        final int cx = width / 2;
        final int cy = height / 2;
        int restoreCount = canvas.save();
        canvas.clipRect(mBounds);

        if (mRunning || (mFinishTime > 0)) {
            long now = AnimationUtils.currentAnimationTimeMillis();
            long elapsed = (now - mStartTime) % ANIMATION_DURATION_MS;
            float rawProgress = (elapsed / (ANIMATION_DURATION_MS / 100f));

            // If we're not running anymore, that means we're running through the finish animation.
            if (!mRunning) {
                // If the finish animation is done, don't draw anything, and don't re-post.
                if ((now - mFinishTime) >= FINISH_ANIMATION_DURATION_MS) {
                    mFinishTime = 0;
                    return;
                }

                // Otherwise, use a 0 opacity alpha layer to clear the animation
                // from the inside out. This layer will prevent the circles from
                // drawing within its bounds.
                long finishElapsed = (now - mFinishTime) % FINISH_ANIMATION_DURATION_MS;
                float finishProgress = (finishElapsed / (FINISH_ANIMATION_DURATION_MS / 100f));
                float pct = (finishProgress / 100f);
                // Radius of the circle is half of the screen.
                float clearRadius = width / 2 * INTERPOLATOR.getInterpolation(pct);
                mClipRect.set(cx - clearRadius, 0, cx + clearRadius, height);
                canvas.saveLayerAlpha(mClipRect, 0, 0);
            }

            if (rawProgress >= 0 && rawProgress < 25) {
                canvas.drawColor(mColor4);
            } else if (rawProgress >= 25 && rawProgress < 50) {
                canvas.drawColor(mColor1);
            } else if (rawProgress >= 50 && rawProgress < 75) {
                canvas.drawColor(mColor2);
            } else {
                canvas.drawColor(mColor3);
            }

            // Then draw up to 4 overlapping concentric circles of varying radii, based on how far
            // along we are in the cycle.
            // progress 0-50 draw mColor2
            // progress 25-75 draw mColor3
            // progress 50-100 draw mColor4
            // progress 75 (wrap to 25) draw mColor1
            if ((rawProgress >= 0 && rawProgress <= 25)) {
                float pct = (((rawProgress + 25) * 2) / 100f);
                drawCircle(canvas, cx, cy, mColor1, pct);
            }
            if (rawProgress >= 0 && rawProgress <= 50) {
                float pct = ((rawProgress * 2) / 100f);
                drawCircle(canvas, cx, cy, mColor2, pct);
            }
            if (rawProgress >= 25 && rawProgress <= 75) {
                float pct = (((rawProgress - 25) * 2) / 100f);
                drawCircle(canvas, cx, cy, mColor3, pct);
            }
            if (rawProgress >= 50 && rawProgress <= 100) {
                float pct = (((rawProgress - 50) * 2) / 100f);
                drawCircle(canvas, cx, cy, mColor4, pct);
            }
            if ((rawProgress >= 75 && rawProgress <= 100)) {
                float pct = (((rawProgress - 75) * 2) / 100f);
                drawCircle(canvas, cx, cy, mColor1, pct);
            }
            // Keep running until we finish out the last cycle.
            ViewCompat.postInvalidateOnAnimation(mParent);
        }
        canvas.restoreToCount(restoreCount);
    }

    /**
     * Draws a circle centered in the view.
     *
     * @param canvas the canvas to draw on
     * @param cx     the center x coordinate
     * @param cy     the center y coordinate
     * @param color  the color to draw
     * @param pct    the percentage of the view that the circle should cover
     */
    private void drawCircle(Canvas canvas, float cx, float cy, int color, float pct) {
        mPaint.setColor(color);
        canvas.save();
        canvas.translate(cx, cy);
        float radiusScale = INTERPOLATOR.getInterpolation(pct);
        canvas.scale(radiusScale, radiusScale);
        canvas.drawCircle(0, 0, cx, mPaint);
        canvas.restore();
    }

    /**
     * Set the drawing bounds of this IndeterminateProgressBar.
     */
    public void setBounds(int right, int bottom) {
        mBounds.left = 0;
        mBounds.top = 0;
        mBounds.right = right;
        mBounds.bottom = bottom;
    }

    final static class BakedBezierInterpolator implements Interpolator {
        private static final BakedBezierInterpolator INSTANCE = new BakedBezierInterpolator();
        /**
         * Lookup table values.
         * Generated using a Bezier curve from (0,0) to (1,1) with control points:
         * P0 (0,0)
         * P1 (0.4, 0)
         * P2 (0.2, 1.0)
         * P3 (1.0, 1.0)
         * <p/>
         * Values sampled with x at regular intervals between 0 and 1.
         */
        private static final float[] VALUES = new float[]{
                0.0f, 0.0002f, 0.0009f, 0.0019f, 0.0036f, 0.0059f, 0.0086f, 0.0119f, 0.0157f, 0.0209f,
                0.0257f, 0.0321f, 0.0392f, 0.0469f, 0.0566f, 0.0656f, 0.0768f, 0.0887f, 0.1033f, 0.1186f,
                0.1349f, 0.1519f, 0.1696f, 0.1928f, 0.2121f, 0.237f, 0.2627f, 0.2892f, 0.3109f, 0.3386f,
                0.3667f, 0.3952f, 0.4241f, 0.4474f, 0.4766f, 0.5f, 0.5234f, 0.5468f, 0.5701f, 0.5933f,
                0.6134f, 0.6333f, 0.6531f, 0.6698f, 0.6891f, 0.7054f, 0.7214f, 0.7346f, 0.7502f, 0.763f,
                0.7756f, 0.7879f, 0.8f, 0.8107f, 0.8212f, 0.8326f, 0.8415f, 0.8503f, 0.8588f, 0.8672f,
                0.8754f, 0.8833f, 0.8911f, 0.8977f, 0.9041f, 0.9113f, 0.9165f, 0.9232f, 0.9281f, 0.9328f,
                0.9382f, 0.9434f, 0.9476f, 0.9518f, 0.9557f, 0.9596f, 0.9632f, 0.9662f, 0.9695f, 0.9722f,
                0.9753f, 0.9777f, 0.9805f, 0.9826f, 0.9847f, 0.9866f, 0.9884f, 0.9901f, 0.9917f, 0.9931f,
                0.9944f, 0.9955f, 0.9964f, 0.9973f, 0.9981f, 0.9986f, 0.9992f, 0.9995f, 0.9998f, 1.0f, 1.0f
        };
        private static final float STEP_SIZE = 1.0f / (VALUES.length - 1);

        /**
         * Use getInstance instead of instantiating.
         */
        private BakedBezierInterpolator() {
            super();
        }

        public static BakedBezierInterpolator getInstance() {
            return INSTANCE;
        }

        @Override
        public float getInterpolation(float input) {
            if (input >= 1.0f) {
                return 1.0f;
            }

            if (input <= 0f) {
                return 0f;
            }

            int position = Math.min((int) (input * (VALUES.length - 1)), VALUES.length - 2);

            float quantized = position * STEP_SIZE;
            float difference = input - quantized;
            float weight = difference / STEP_SIZE;

            return VALUES[position] + weight * (VALUES[position + 1] - VALUES[position]);
        }

    }
}