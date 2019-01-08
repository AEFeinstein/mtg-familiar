/*
 * Copyright 2017 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import com.tokenautocomplete.TokenCompleteTextView;

public abstract class ATokenTextView extends TokenCompleteTextView<String> {

    private boolean handleDismiss = false;
    boolean mAllowDuplicates = false;

    public ATokenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.performBestGuess(false);
        this.allowCollapse(true);
        this.setTokenClickStyle(TokenClickStyle.Delete);
    }

    @Override
    protected String defaultObject(String completionText) {
        return "";
    }

    @Override
    public boolean enoughToFilter() {
        return isFocused();
    }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        try {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused && this.getAdapter() != null) {
                performFiltering(getText(), 0);
            }
        } catch (IndexOutOfBoundsException e) {
            // Ignore it
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        handleDismiss = true;
        super.onWindowFocusChanged(hasWindowFocus);
        handleDismiss = false;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        handleDismiss = true;
        boolean result = super.onKeyPreIme(keyCode, event);
        handleDismiss = false;
        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
        handleDismiss = true;
        super.onDetachedFromWindow();
        handleDismiss = false;
    }

    public void dismissDropDown() {
        if (handleDismiss) {
            super.dismissDropDown();
        }
    }

    /**
     * This checks if tokens should be added or not. If duplicates aren't allowed, dont add any
     * tokens which have already been added
     *
     * @param token The token which is trying to be added
     * @return true to allow this token to be added, false otherwise
     */
    @Override
    public boolean shouldIgnoreToken(String token) {
        if (!mAllowDuplicates) {
            return getObjects().contains(token);
        }
        return false;
    }
}
