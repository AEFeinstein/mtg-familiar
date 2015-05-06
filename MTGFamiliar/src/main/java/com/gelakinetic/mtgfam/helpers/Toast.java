package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

/**
 * Toast that cancels when new toast shows
 */
public class Toast extends android.widget.Toast {

    private static android.widget.Toast toast;

    public Toast(Context context) {
        super(context);
    }

    /**
     * Cancel current toast if present
     */
    public static void cancelToast() {
        if (toast != null) {
            toast.cancel();
        }
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    @NonNull
    public static android.widget.Toast makeText(@NonNull Context context, CharSequence text,
                                                int duration) {
        if (toast != null) {
            toast.cancel();
        }
        toast = android.widget.Toast.makeText(context, text, duration);
        return toast;
    }

    /**
     * Make a standard toast that just contains a text view with the text from a resource.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @NonNull
    public static android.widget.Toast makeText(@NonNull Context context, int resId,
                                                int duration)
            throws Resources.NotFoundException {
        if (toast != null) {
            toast.cancel();
        }
        toast = android.widget.Toast.makeText(context, resId, duration);
        return toast;
    }

}
