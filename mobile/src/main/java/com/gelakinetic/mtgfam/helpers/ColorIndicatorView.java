package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.view.View;

import com.gelakinetic.mtgfam.R;

import java.util.Arrays;

public class ColorIndicatorView extends View {
    private ShapeDrawable mBackground = null;
    private ShapeDrawable mDrawableShapes[] = new ShapeDrawable[5];

    private static final String COLORS_CHARS[] = {"w", "u", "b", "r", "g"};
    private static final int COLOR_RESOURCES[] = {
            R.color.icon_white,
            R.color.icon_blue,
            R.color.icon_black,
            R.color.icon_red,
            R.color.icon_green
    };

    /**
     * Necessary constructor
     *
     * @param context The context
     */
    public ColorIndicatorView(Context context) {
        this(context, 0, 0, "", "");
    }

    /**
     * Create a color indicator view
     *
     * @param context The context
     * @param dimen   The width & height of the view, in pixels
     * @param border  How thick the border should be, in pixels
     * @param color   A string of characters representing colors
     * @param manacost The mana cost of this card. If it matches the color, don't make an indicator
     */
    public ColorIndicatorView(Context context, int dimen, int border, String color, String manacost) {
        super(context);

        int shapesIndex = 0;
        int numColors = 0;

        /* Sanitize strings to check for a match */
        manacost = sanitizeString(manacost);
        color = sanitizeString(color);

        if(color.equals(manacost)) {
            /* Color matches manacost, don't bother with an indicator */
            return;
        }

        /* Count the number of colors, ignoring artifact and land */
        for (String colorChar : COLORS_CHARS) {
            if (color.contains(colorChar)) {
                numColors++;
            }
        }

        /* No colors? Don't bother with wedges or a background */
        if (numColors == 0) {
            return;
        }

        /* For each color, draw a slice of the pie, rotated a bit for niceness */
        for (int i = 0; i < COLORS_CHARS.length; i++) {
            if (color.contains(COLORS_CHARS[i] + "")) {
                mDrawableShapes[shapesIndex] = new ShapeDrawable(new ArcShape(shapesIndex * (360 / numColors) + 135, (360 / numColors)));
                mDrawableShapes[shapesIndex].getPaint().setColor(context.getResources().getColor(COLOR_RESOURCES[i]));
                mDrawableShapes[shapesIndex].setBounds(border, border, dimen - border, dimen - border);
                shapesIndex++;
            }
        }

        /* Set up a border for the indicator, helps to see white */
        mBackground = new ShapeDrawable(new ArcShape(0, 360));
        mBackground.getPaint().setColor(context.getResources().getColor(android.R.color.black));
        mBackground.setBounds(0, 0, dimen, dimen);
    }

    /**
     * Draw the background, then draw the slices of the indicator
     *
     * @param canvas A canvas to draw on
     */
    protected void onDraw(Canvas canvas) {
        if(mBackground != null) {
            mBackground.draw(canvas);
        }
        for (ShapeDrawable shape : mDrawableShapes) {
            if (shape != null) {
                shape.draw(canvas);
            }
        }
    }

    /**
     * Returns whether or not the indicator should be shown
     * @return true if it should be shown, false otherwise
     */
    public boolean shouldInidcatorBeShown() {
        return mBackground != null;
    }

    /**
     * Given a color or mana cost string, remove all non-color & duplicate chars
     * @param str A mana cost or color string
     * @return The sanitized string
     */
    public static String sanitizeString(String str) {
        str = str.toLowerCase();
        boolean colors[] = new boolean[5];
        Arrays.fill(colors, false);

        for (int i = 0; i < str.length(); i++) {
            switch(str.charAt(i))
            {
                case 'w':
                    colors[0] = true;
                    break;
                case 'u':
                    colors[1] = true;
                    break;
                case 'b':
                    colors[2] = true;
                    break;
                case 'r':
                    colors[3] = true;
                    break;
                case 'g':
                    colors[4] = true;
                    break;
            }
        }

        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < colors.length; i++) {
            if (colors[i]) {
                switch(i)
                {
                    case 0:
                        sb.append('w');
                        break;
                    case 1:
                        sb.append('u');
                        break;
                    case 2:
                        sb.append('b');
                        break;
                    case 3:
                        sb.append('r');
                        break;
                    case 4:
                        sb.append('g');
                        break;
                }
            }
        }

        return sb.toString();
    }
}