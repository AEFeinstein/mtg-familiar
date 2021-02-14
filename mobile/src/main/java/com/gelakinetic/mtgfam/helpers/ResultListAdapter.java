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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.Locale;

/**
 * This list adapter is used to display a list of search results. It implements SectionIndexer to enable fast scrolling.
 */
public class ResultListAdapter extends SimpleCursorAdapter {

    private final String[] mFrom;
    private final int[] mTo;
    private final ImageGetter mImgGetter;
    private final Resources.Theme mTheme;

    /**
     * Standard Constructor.
     *
     * @param context The context where the ListView associated with this SimpleListItemFactory is running
     * @param cursor  The database cursor. Can be null if the cursor is not available yet.
     * @param from    A list of column names representing the data to bind to the UI. Can be null if the cursor is not
     *                available yet.
     * @param to      The views that should display column in the "from" parameter. These should all be TextViews. The
     *                first N views in this list are given the values of the first N columns in the from parameter.
     *                Can be null if the cursor is not available yet.
     */
    public ResultListAdapter(Context context, Cursor cursor, String[] from, int[] to) {
        super(context, R.layout.result_list_card_row, cursor, from, to, 0);
        this.mFrom = from;
        this.mTo = to;
        this.mTheme = context.getTheme();
        this.mImgGetter = ImageGetterHelper.GlyphGetter(context);
    }

    /**
     * Inflates view(s) from the specified XML file.
     *
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the inflated view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.result_list_card_row, parent, false);
    }

    /**
     * Binds all of the field names passed into the "to" parameter of the constructor with their corresponding cursor
     * columns as specified in the "from" parameter. Binding occurs in two phases. First, if a
     * SimpleCursorAdapter.ViewBinder is available, setViewValue(android.view.View, android.database.Cursor, int) is
     * invoked. If the returned value is true, binding has occurred. If the returned value is false and the view to bind
     * is a TextView, setViewText(TextView, String) is invoked. If the returned value is false and the view to bind is
     * an ImageView, setViewImage(ImageView, String) is invoked. If no appropriate binding can be found, an
     * IllegalStateException is thrown.
     *
     * @param view    Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
     */
    @Override
    public void bindView(@NonNull View view, Context context, @NonNull Cursor cursor) {

        boolean hideCost = true;
        boolean hideType = true;
        boolean hideAbility = true;
        boolean hidePT = true;
        boolean hideLoyalty = true;

        /* make sure these elements are showing (views get recycled) */
        view.findViewById(R.id.cardp).setVisibility(View.VISIBLE);
        view.findViewById(R.id.cardslash).setVisibility(View.VISIBLE);
        view.findViewById(R.id.cardt).setVisibility(View.VISIBLE);

        /* Iterate through the mFrom, find the appropriate view in mTo */
        for (int i = 0; i < mFrom.length; i++) {

            TextView textField = view.findViewById(mTo[i]);

            switch (mFrom[i]) {
                case CardDbAdapter.KEY_NAME: {
                    String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
                    textField.setText(name);
                    break;
                }
                case CardDbAdapter.KEY_MANACOST: {
                    String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
                    hideCost = false;
                    CharSequence csq = ImageGetterHelper.formatStringWithGlyphs(name, mImgGetter);
                    textField.setText(csq);
                    break;
                }
                case CardDbAdapter.KEY_SET: {
                    char rarity = (char) cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
                    String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
                    textField.setText(name);
                    switch (rarity) {
                        case 'c':
                        case 'C':
                            textField.setTextColor(ContextCompat.getColor(context, getResourceIdFromAttr(R.attr.color_common)));
                            break;
                        case 'u':
                        case 'U':
                            textField.setTextColor(ContextCompat.getColor(context, getResourceIdFromAttr(R.attr.color_uncommon)));
                            break;
                        case 'r':
                        case 'R':
                            textField.setTextColor(ContextCompat.getColor(context, getResourceIdFromAttr(R.attr.color_rare)));
                            break;
                        case 'm':
                        case 'M':
                            textField.setTextColor(ContextCompat.getColor(context, getResourceIdFromAttr(R.attr.color_mythic)));
                            break;
                        case 't':
                        case 'T':
                            textField.setTextColor(ContextCompat.getColor(context, getResourceIdFromAttr(R.attr.color_timeshifted)));
                            break;
                    }

                    if (PreferenceAdapter.getSetPref(context)) {
                        ExpansionImageHelper.loadExpansionImage(context, name, rarity, view.findViewById(R.id.cardsetimage), view.findViewById(R.id.cardset), ExpansionImageHelper.ExpansionImageSize.LARGE);
                    }
                    break;
                }
                case CardDbAdapter.KEY_RARITY: {
                    char rarity = (char) cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
                    textField.setText(String.format(Locale.getDefault(), "(%c)", rarity));
                    break;
                }
                case CardDbAdapter.KEY_SUPERTYPE: {
                    String name = CardDbAdapter.getTypeLine(cursor);
                    hideType = false;
                    textField.setText(name);
                    break;
                }
                case CardDbAdapter.KEY_ABILITY: {
                    String name = cursor.getString(cursor.getColumnIndex(mFrom[i]));
                    hideAbility = false;
                    CharSequence csq = ImageGetterHelper.formatStringWithGlyphs(name, mImgGetter);
                    textField.setText(csq);
                    break;
                }
                case CardDbAdapter.KEY_POWER:
                case CardDbAdapter.KEY_TOUGHNESS: {
                    float p = cursor.getFloat(cursor.getColumnIndex(mFrom[i]));
                    boolean shouldShowSign =
                            cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_SET)).equals("UST") &&
                                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_ABILITY)).contains("Augment {");
                    if (p != CardDbAdapter.NO_ONE_CARES) {
                        hidePT = false;
                        textField.setText(CardDbAdapter.getPrintedPTL(p, shouldShowSign));
                    }
                    break;
                }
                case CardDbAdapter.KEY_LOYALTY: {
                    float l = cursor.getFloat(cursor.getColumnIndex(mFrom[i]));
                    if (l != CardDbAdapter.NO_ONE_CARES) {
                        hideLoyalty = false;
                        ((TextView) textField.findViewById(R.id.cardt)).setText(CardDbAdapter.getPrintedPTL(l, false));
                    }
                    break;
                }
            }
        }

        /* Hide the fields if they should be hidden (didn't exist in mTo)*/
        if (hideCost) {
            view.findViewById(R.id.cardcost).setVisibility(View.GONE);
        }
        if (PreferenceAdapter.getSetPref(context)) {
            view.findViewById(R.id.cardsetcombo).setVisibility(View.VISIBLE);
            view.findViewById(R.id.rarity).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.cardsetcombo).setVisibility(View.GONE);
            view.findViewById(R.id.rarity).setVisibility(View.GONE);
        }
        if (hideType) {
            view.findViewById(R.id.cardtype).setVisibility(View.GONE);
        }
        if (hideAbility) {
            view.findViewById(R.id.cardability).setVisibility(View.GONE);
        }
        if (!hideLoyalty) {
            view.findViewById(R.id.cardp).setVisibility(View.GONE);
            view.findViewById(R.id.cardslash).setVisibility(View.GONE);
        } else if (hidePT) {
            view.findViewById(R.id.cardp).setVisibility(View.GONE);
            view.findViewById(R.id.cardslash).setVisibility(View.GONE);
            view.findViewById(R.id.cardt).setVisibility(View.GONE);
        }
    }

    /**
     * This helper function translates an attribute into a resource ID
     *
     * @param attr The attribute ID
     * @return the resource ID
     */
    private int getResourceIdFromAttr(int attr) {
        TypedArray ta = mTheme.obtainStyledAttributes(new int[]{attr});
        int resId = ta.getResourceId(0, 0);
        ta.recycle();
        return resId;
    }
}