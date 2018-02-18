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
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

public class FamiliarGlideTarget extends SimpleTarget<Drawable> {
    final FamiliarActivity mActivity;
    final ImageView mImageView;

    public FamiliarGlideTarget(FamiliarActivity activity, ImageView imageView) {
        mActivity = activity;
        mImageView = imageView;
    }

    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        mActivity.setLoading();
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        mImageView.setImageDrawable(resource);
        mActivity.clearLoading();
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        mActivity.clearLoading();
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        mActivity.clearLoading();
        ToastWrapper.makeAndShowText(mActivity, R.string.card_view_image_not_found, ToastWrapper.LENGTH_SHORT);
        // TODO try loading next URL, only show toast when all have been exhausted
    }
}
