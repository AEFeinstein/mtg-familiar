/**
 * Copyright 2011 Adam Feinstein
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.SpannedString;

import com.gelakinetic.mtgfam.R;

/**
 * This class replaces text with mana symbols and other glyphs. It does so using Html.fromHtml and custom-processing
 * <img> tags
 * TODO make the whole thing static? subclass ImageGetter?
 */
public class ImageGetterHelper {

    private final static int[] drawableNumbers = {R.drawable.glyph_0, R.drawable.glyph_1, R.drawable.glyph_2,
            R.drawable.glyph_3, R.drawable.glyph_4, R.drawable.glyph_5, R.drawable.glyph_6, R.drawable.glyph_7,
            R.drawable.glyph_8, R.drawable.glyph_9, R.drawable.glyph_10, R.drawable.glyph_11, R.drawable.glyph_12,
            R.drawable.glyph_13, R.drawable.glyph_14, R.drawable.glyph_15, R.drawable.glyph_16, R.drawable.glyph_17,
            R.drawable.glyph_18, R.drawable.glyph_19, R.drawable.glyph_20};

    /**
     * Jellybean had a weird bug, and this fixes it. Silly google!
     * https://code.google.com/p/android/issues/detail?id=35466#c2
     *
     * @param source      The string to add glyphs to
     * @param imageGetter the custom imageGetter, returned by GlyphGetter
     * @return the Spanned with shiny new glyphs
     */
    public static Spanned formatStringWithGlyphs(String source, ImageGetter imageGetter) {
        /* Make sure we're not formatting a null string */
        if (source == null) {
            return new SpannedString("");
        }
        source = source.replace("{", "<img src=\"").replace("}", "\"/>");
        if (Build.VERSION.SDK_INT == 16) {
            source = source.replace("<", " <").replace(">", " >").replace("  ", " ");
        }
        return Html.fromHtml(source, imageGetter, null);
    }

    /**
     * Same weird bug as above, but without the custom imageGetter / tagHandler
     *
     * @param source A string of HTML
     * @return a formatted Spanned which JellyBean is happy with
     */
    public static Spanned formatHtmlString(String source) {
        /* Make sure we're not formatting a null string */
        if (source == null) {
            return new SpannedString("");
        }
        if (Build.VERSION.SDK_INT == 16) {
            source = source.replace("<", " <").replace(">", " >").replace("  ", " ");
        }
        return Html.fromHtml(source);
    }

    /**
     * This function returns a custom ImageGetter which replaces glyphs with text.
     * This could have been a new subclass of ImageGetter with a constructor that takes a resource, but I guess
     * I didn't feel like writing it that way on that day.
     *
     * @param context the context to get resources to get drawables from
     * @return a custom ImageGetter
     */
    public static ImageGetter GlyphGetter(final Context context) {
        return new ImageGetter() {
            public Drawable getDrawable(String source) {
                Drawable d = null;
                source = source.replace("/", "");

                if (source.equalsIgnoreCase("w")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_w);//ContextCompat.getDrawable(context, R.drawable.glyph_w, null);
                } else if (source.equalsIgnoreCase("u")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_u);
                } else if (source.equalsIgnoreCase("b")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_b);
                } else if (source.equalsIgnoreCase("r")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_r);
                } else if (source.equalsIgnoreCase("g")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_g);
                } else if (source.equalsIgnoreCase("t")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_tap);
                } else if (source.equalsIgnoreCase("q")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_untap);
                } else if (source.equalsIgnoreCase("wu") || source.equalsIgnoreCase("uw")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_wu);
                } else if (source.equalsIgnoreCase("ub") || source.equalsIgnoreCase("bu")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_ub);
                } else if (source.equalsIgnoreCase("br") || source.equalsIgnoreCase("rb")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_br);
                } else if (source.equalsIgnoreCase("rg") || source.equalsIgnoreCase("gr")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_rg);
                } else if (source.equalsIgnoreCase("gw") || source.equalsIgnoreCase("wg")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_gw);
                } else if (source.equalsIgnoreCase("wb") || source.equalsIgnoreCase("bw")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_wb);
                } else if (source.equalsIgnoreCase("bg") || source.equalsIgnoreCase("gb")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_bg);
                } else if (source.equalsIgnoreCase("gu") || source.equalsIgnoreCase("ug")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_gu);
                } else if (source.equalsIgnoreCase("ur") || source.equalsIgnoreCase("ru")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_ur);
                } else if (source.equalsIgnoreCase("rw") || source.equalsIgnoreCase("wr")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_rw);
                } else if (source.equalsIgnoreCase("2w") || source.equalsIgnoreCase("w2")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_w2);
                } else if (source.equalsIgnoreCase("2u") || source.equalsIgnoreCase("u2")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_u2);
                } else if (source.equalsIgnoreCase("2b") || source.equalsIgnoreCase("b2")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_b2);
                } else if (source.equalsIgnoreCase("2r") || source.equalsIgnoreCase("r2")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_r2);
                } else if (source.equalsIgnoreCase("2g") || source.equalsIgnoreCase("g2")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_g2);
                } else if (source.equalsIgnoreCase("s")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_s);
                } else if (source.equalsIgnoreCase("pw") || source.equalsIgnoreCase("wp")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_pw);
                } else if (source.equalsIgnoreCase("pu") || source.equalsIgnoreCase("up")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_pu);
                } else if (source.equalsIgnoreCase("pb") || source.equalsIgnoreCase("bp")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_pb);
                } else if (source.equalsIgnoreCase("pr") || source.equalsIgnoreCase("rp")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_pr);
                } else if (source.equalsIgnoreCase("pg") || source.equalsIgnoreCase("gp")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_pg);
                } else if (source.equalsIgnoreCase("p")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_p);
                } else if (source.equalsIgnoreCase("+oo")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_inf);
                } else if (source.equalsIgnoreCase("100")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_100);
                } else if (source.equalsIgnoreCase("1000000")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_1000000);
                } else if (source.equalsIgnoreCase("hr") || source.equalsIgnoreCase("rh")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_hr);
                } else if (source.equalsIgnoreCase("hw") || source.equalsIgnoreCase("wh")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_hw);
                } else if (source.equalsIgnoreCase("c") || source.equalsIgnoreCase("chaos")) {
                    d = ContextCompat.getDrawable(context, getResourceIdFromAttr(context.getTheme(), R.attr.glyph_c));
                } else if (source.equalsIgnoreCase("z")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_z);
                } else if (source.equalsIgnoreCase("y")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_y);
                } else if (source.equalsIgnoreCase("x")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_x);
                } else if (source.equalsIgnoreCase("h")) {
                    d = ContextCompat.getDrawable(context, R.drawable.glyph_half);
                } else if (source.equalsIgnoreCase("pwk")) {
                    d = ContextCompat.getDrawable(context, getResourceIdFromAttr(context.getTheme(), R.attr.glyph_pwk));
                } else {
                    for (int i = 0; i < drawableNumbers.length; i++) {
                        if (source.equals(Integer.valueOf(i).toString())) {
                            d = ContextCompat.getDrawable(context, drawableNumbers[i]);
                        }
                    }
                }

                if (d == null) {
                    return null;
                }

                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                return d;
            }
        };
    }

    /**
     * This helper function translates an attribute into a resource ID
     *
     * @param attr The attribute ID
     * @return the resource ID
     */
    private static int getResourceIdFromAttr(Resources.Theme theme, int attr) {
        TypedArray ta = theme.obtainStyledAttributes(new int[]{attr});
        assert ta != null;
        int resId = ta.getResourceId(0, 0);
        ta.recycle();
        return resId;
    }
}