/*
 * Copyright 2018 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;

public class FamiliarGlideTarget extends SimpleTarget<Drawable> {

    public static abstract class DrawableLoadedCallback {
        /**
         * A callback to call if we want to save the drawable rather than display it
         *
         * @param resource The Drawable that is loaded
         */
        protected abstract void onDrawableLoaded(Drawable resource);
    }

    private final CardViewFragment mFragment;
    private final ImageView mImageView;
    private final int mAttempt;
    private final DrawableLoadedCallback mDrawableLoadedCallback;
    private final int mWhereTo;

    /**
     * Constructor for loading an image into an ImageView
     *
     * @param fragment  The fragment that hosts the ImageView
     * @param imageView The ImageView to load into
     * @param attempt   The attempt number. Should start at 0 and will increment through the
     *                  different image sources
     */
    public FamiliarGlideTarget(CardViewFragment fragment, ImageView imageView, int attempt) {
        mFragment = fragment;
        mImageView = imageView;
        mAttempt = attempt;
        mDrawableLoadedCallback = null;
        mWhereTo = 0;
    }

    /**
     * Constructor for saving images to files
     *
     * @param fragment The fragment that the image is being saved from
     * @param callback A callback to be called when the image is loaded
     * @param whereTo  What to do with the loaded image, either MAIN_PAGE to save it or SHARE to
     *                 share it
     * @param attempt  The attempt number. Should start at 0 and will increment through the
     *                 different image sources
     */
    public FamiliarGlideTarget(CardViewFragment fragment, DrawableLoadedCallback callback,
                               int whereTo, int attempt) {
        mFragment = fragment;
        mImageView = null;
        mAttempt = attempt;
        mDrawableLoadedCallback = callback;
        mWhereTo = whereTo;
    }

    /**
     * When the load starts, have the activity display the loading animation
     *
     * @param placeholder unused
     */
    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        mFragment.getFamiliarActivity().setLoading();
    }

    /**
     * When the resource is ready, either set it to the ImageView or call the callback to save it
     * Also clear the loading animation from the activity
     *
     * @param resource   The Drawable that was loaded
     * @param transition unused
     */
    @Override
    public void onResourceReady(@NonNull Drawable resource,
                                @Nullable Transition<? super Drawable> transition) {
        if (null != mImageView) {
            mImageView.setImageDrawable(resource);
        } else if (null != mDrawableLoadedCallback) {
            mDrawableLoadedCallback.onDrawableLoaded(resource);
        }
        mFragment.getFamiliarActivity().clearLoading();
    }

    /**
     * When the load is cleared, have the activity clear the loading animation
     *
     * @param placeholder unused
     */
    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        mFragment.getFamiliarActivity().clearLoading();
    }

    /**
     * If the load failed, try loading it again with the next attempt. If all attempts fail, it'll
     * be handled by the caller
     *
     * @param errorDrawable unused
     */
    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        // Try to load the next URL
        if (null != mImageView) {
            mFragment.loadImageWithGlide(mImageView, mAttempt + 1);
        } else if (null != mDrawableLoadedCallback) {
            mFragment.saveImageWithGlide(mWhereTo, mAttempt + 1);
        }
    }
}
