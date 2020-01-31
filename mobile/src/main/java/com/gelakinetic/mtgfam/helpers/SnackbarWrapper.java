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

package com.gelakinetic.mtgfam.helpers;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;

import androidx.annotation.StringRes;

import com.gelakinetic.mtgfam.R;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

/**
 * Snackbar that cancels when new snackbar shows
 */
public class SnackbarWrapper {

    public static final int LENGTH_LONG = Snackbar.LENGTH_LONG;
    public static final int LENGTH_SHORT = Snackbar.LENGTH_SHORT;
    public static final int LENGTH_XLONG = 2750 * 3; // Three times long, see SnackbarManager.LONG_DURATION_MS

    private static WeakReference<Snackbar> mSnackbar;

    /**
     * Cancel current snackbar if present
     */
    public static void cancelSnackbar() {
        if (mSnackbar != null && mSnackbar.get() != null) {
            mSnackbar.get().dismiss();
        }
    }

    /**
     * Make a standard snackbar that just contains a text view.
     *
     * @param activity The activity to find a parent from.
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    public static void makeAndShowText(Activity activity, CharSequence text, int duration) {
        makeAndShowText(activity, text, duration, 0, null, null);
    }

    /**
     * Make a standard snackbar that just contains a text view with the text from a resource.
     *
     * @param activity The activity to find a parent from.
     * @param resId    The resource id of the string resource to use.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void makeAndShowText(Activity activity, @StringRes int resId, int duration)
            throws Resources.NotFoundException {
        if (activity != null) {
            makeAndShowText(activity, activity.getString(resId), duration);
        }
    }

    /**
     * Make a standard snackbar with possible actions and callbacks
     *
     * @param activity          The activity to find a parent from.
     * @param text              The text to show.  Can be formatted text.
     * @param duration          How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                          {@link #LENGTH_LONG}
     * @param actionStringResId String resource to display for the action
     * @param actionListener    callback to be invoked when the action is clicked
     * @param callback          Callback to notify when transient bottom bar events occur.
     */
    public static void makeAndShowText(Activity activity, CharSequence text, int duration,
                                       @StringRes int actionStringResId,
                                       View.OnClickListener actionListener, Snackbar.Callback callback) {
        if (mSnackbar != null && mSnackbar.get() != null) {
            mSnackbar.get().dismiss();
        }
        if (activity != null && !activity.isFinishing()) {
            mSnackbar = new WeakReference<>(Snackbar.make(activity.findViewById(R.id.myCoordinatorLayout), text, duration));
            if (null != actionListener) {
                mSnackbar.get().setAction(actionStringResId, actionListener);
            }
            if (null != callback) {
                mSnackbar.get().addCallback(callback);
            }
            mSnackbar.get().show();
        }
    }
}
