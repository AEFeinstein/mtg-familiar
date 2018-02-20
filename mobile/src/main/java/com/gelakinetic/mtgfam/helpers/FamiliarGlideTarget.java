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
    private final CardViewFragment mFragment;
    private final ImageView mImageView;
    private final int mAttempt;

    public FamiliarGlideTarget(CardViewFragment fragment, ImageView imageView, int attempt) {
        mFragment = fragment;
        mImageView = imageView;
        mAttempt = attempt;
    }

    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        mFragment.getFamiliarActivity().setLoading();
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        mImageView.setImageDrawable(resource);
        mFragment.getFamiliarActivity().clearLoading();
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        mFragment.getFamiliarActivity().clearLoading();
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        // Try to load the next URL
        mFragment.loadImageWithGlide(mImageView, mAttempt + 1);
    }
}
