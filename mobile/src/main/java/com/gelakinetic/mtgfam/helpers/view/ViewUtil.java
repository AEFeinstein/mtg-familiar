package com.gelakinetic.mtgfam.helpers.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.jetbrains.annotations.Contract;

class ViewUtil {
    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    static float convertDpToPixel(float dp, Context context){
        final float densityDpi = ViewUtil.getDensityDpi(context);
        return dp * densityDpi / DisplayMetrics.DENSITY_DEFAULT;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        final float densityDpi = ViewUtil.getDensityDpi(context);
        return px * DisplayMetrics.DENSITY_DEFAULT / densityDpi;
    }

    /**
     * Scales dimTwoBefore to the ratio of dimOneBefore and dimOneAfter.
     * @param dimOneBefore dimension 1 before resize.
     * @param dimOneAfter dimension 1 after resize.
     * @param dimTwoBefore dimension 2 before resize.
     * @return dimension 2 after resize.
     */
    @Contract(pure = true)
    static float scaleDimension(float dimOneBefore, float dimOneAfter, float dimTwoBefore) {
        if (dimOneBefore == 0) {
            return 0;
        }
        return dimTwoBefore * dimOneAfter / dimOneBefore;
    }

    private static float getDensityDpi(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return metrics.densityDpi;
    }
}
