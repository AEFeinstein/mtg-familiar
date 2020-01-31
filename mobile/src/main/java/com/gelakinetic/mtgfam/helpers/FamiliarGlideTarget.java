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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.CardViewDialogFragment;

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
    private final DrawableLoadedCallback mDrawableLoadedCallback;

    /**
     * Constructor for loading an image into an ImageView
     *
     * @param fragment  The fragment that hosts the ImageView
     * @param imageView The ImageView to load into
     */
    public FamiliarGlideTarget(CardViewFragment fragment, ImageView imageView) {
        mFragment = fragment;
        mImageView = imageView;
        mDrawableLoadedCallback = null;
    }

    /**
     * Constructor for saving images to files
     *
     * @param fragment The fragment that the image is being saved from
     * @param callback A callback to be called when the image is loaded
     */
    public FamiliarGlideTarget(CardViewFragment fragment, DrawableLoadedCallback callback) {
        mFragment = fragment;
        mImageView = null;
        mDrawableLoadedCallback = callback;
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
            // Load directly to the fragment
            mImageView.setImageDrawable(resource);
        } else if (null != mDrawableLoadedCallback) {
            // Call a callback to save/share the image
            mDrawableLoadedCallback.onDrawableLoaded(resource);
        } else {
            // Save the drawable in RAM, launch a dialog to display it
            mFragment.setImageDrawableForDialog(resource);
            mFragment.showDialog(CardViewDialogFragment.GET_IMAGE);
        }
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
        mFragment.getFamiliarActivity().clearLoading();
        SnackbarWrapper.makeAndShowText(mFragment.getActivity(), R.string.card_view_image_not_found, SnackbarWrapper.LENGTH_SHORT);
        mFragment.showText();
    }
}
