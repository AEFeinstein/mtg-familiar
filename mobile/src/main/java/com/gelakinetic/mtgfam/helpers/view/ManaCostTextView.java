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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.R;
import com.tokenautocomplete.FilteredArrayAdapter;

import java.util.LinkedHashMap;
import java.util.Objects;

public class ManaCostTextView extends ATokenTextView {
    private static final LinkedHashMap<String, BitmapDrawable> MANA_DRAWABLES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Integer> MANA_SYMBOLS = new LinkedHashMap<>();

    static {
        MANA_SYMBOLS.put("0", R.drawable.glyph_0);
        MANA_SYMBOLS.put("1", R.drawable.glyph_1);
        MANA_SYMBOLS.put("2", R.drawable.glyph_2);
        MANA_SYMBOLS.put("3", R.drawable.glyph_3);
        MANA_SYMBOLS.put("4", R.drawable.glyph_4);
        MANA_SYMBOLS.put("5", R.drawable.glyph_5);
        MANA_SYMBOLS.put("6", R.drawable.glyph_6);
        MANA_SYMBOLS.put("7", R.drawable.glyph_7);
        MANA_SYMBOLS.put("8", R.drawable.glyph_8);
        MANA_SYMBOLS.put("9", R.drawable.glyph_9);
        MANA_SYMBOLS.put("10", R.drawable.glyph_10);
        MANA_SYMBOLS.put("11", R.drawable.glyph_11);
        MANA_SYMBOLS.put("12", R.drawable.glyph_12);
        MANA_SYMBOLS.put("13", R.drawable.glyph_13);
        MANA_SYMBOLS.put("15", R.drawable.glyph_15);
        MANA_SYMBOLS.put("16", R.drawable.glyph_16);
        MANA_SYMBOLS.put("1000000", R.drawable.glyph_1000000);
        MANA_SYMBOLS.put("X", R.drawable.glyph_x);
        MANA_SYMBOLS.put("Y", R.drawable.glyph_y);
        MANA_SYMBOLS.put("Z", R.drawable.glyph_z);
        MANA_SYMBOLS.put("C", R.drawable.glyph_c);
        MANA_SYMBOLS.put("W", R.drawable.glyph_w);
        MANA_SYMBOLS.put("U", R.drawable.glyph_u);
        MANA_SYMBOLS.put("B", R.drawable.glyph_b);
        MANA_SYMBOLS.put("R", R.drawable.glyph_r);
        MANA_SYMBOLS.put("G", R.drawable.glyph_g);
        MANA_SYMBOLS.put("2W", R.drawable.glyph_w2);
        MANA_SYMBOLS.put("2U", R.drawable.glyph_u2);
        MANA_SYMBOLS.put("2B", R.drawable.glyph_b2);
        MANA_SYMBOLS.put("2R", R.drawable.glyph_r2);
        MANA_SYMBOLS.put("2G", R.drawable.glyph_g2);
        MANA_SYMBOLS.put("GW", R.drawable.glyph_gw);
        MANA_SYMBOLS.put("WU", R.drawable.glyph_wu);
        MANA_SYMBOLS.put("UB", R.drawable.glyph_ub);
        MANA_SYMBOLS.put("BR", R.drawable.glyph_br);
        MANA_SYMBOLS.put("RG", R.drawable.glyph_rg);
        MANA_SYMBOLS.put("GU", R.drawable.glyph_gu);
        MANA_SYMBOLS.put("UR", R.drawable.glyph_ur);
        MANA_SYMBOLS.put("RW", R.drawable.glyph_rw);
        MANA_SYMBOLS.put("WB", R.drawable.glyph_wb);
        MANA_SYMBOLS.put("BG", R.drawable.glyph_bg);
        MANA_SYMBOLS.put("WP", R.drawable.glyph_pw);
        MANA_SYMBOLS.put("UP", R.drawable.glyph_pu);
        MANA_SYMBOLS.put("BP", R.drawable.glyph_pb);
        MANA_SYMBOLS.put("RP", R.drawable.glyph_pr);
        MANA_SYMBOLS.put("GP", R.drawable.glyph_pg);
        MANA_SYMBOLS.put("HW", R.drawable.glyph_hw);
    }

    public ManaCostTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAllowDuplicates = true;
        ManaSymbolAdapter manaSymbolAdapter = new ManaSymbolAdapter();
        this.setAdapter(manaSymbolAdapter);
    }

    @Override
    protected View getViewForObject(String symbol) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        ImageView view = (ImageView) Objects.requireNonNull(l).inflate(R.layout.mana_token, (ViewGroup) getParent(), false);
        BitmapDrawable bitmapDrawable = MANA_DRAWABLES.get(symbol);
        if (bitmapDrawable == null) {
            int resId = MANA_SYMBOLS.get(symbol);
            final Bitmap bitmap = getBitmapFromVectorDrawable(getContext(), resId);
            float bitmapHeight = ViewUtil.convertDpToPixel(15, this.getContext());
            float bitmapWidth = ViewUtil.scaleDimension(bitmap.getHeight(), bitmapHeight,
                    bitmap.getWidth());
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) bitmapWidth,
                    (int) bitmapHeight, false);
            bitmapDrawable = new BitmapDrawable(this.getResources(), scaledBitmap);
            MANA_DRAWABLES.put(symbol, bitmapDrawable);
        }
        view.setImageDrawable(bitmapDrawable);
        return view;
    }

    private class ManaSymbolAdapter extends FilteredArrayAdapter<String> {

        ManaSymbolAdapter() {
            super(ManaCostTextView.this.getContext(), R.layout.list_item_1,
                    MANA_SYMBOLS.keySet().toArray(new String[0]));
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            String symbol = super.getItem(position);
            int resId = MANA_SYMBOLS.get(symbol);
            view.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
            view.setCompoundDrawablePadding(10);
            return view;
        }

        @Override
        protected boolean keepObject(String obj, String mask) {
            return obj.toUpperCase().contains(mask.toUpperCase());
        }
    }

    private static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(Objects.requireNonNull(drawable).getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
