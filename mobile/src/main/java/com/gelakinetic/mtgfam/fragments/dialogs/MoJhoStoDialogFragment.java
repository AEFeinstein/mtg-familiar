package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for MoJhoStoFragment
 */
public class MoJhoStoDialogFragment extends FamiliarDialogFragment {

    /* Dialog Constants */
    public static final int DIALOG_RULES = 1;
    public static final int DIALOG_MOMIR = 2;
    public static final int DIALOG_STONEHEWER = 3;
    public static final int DIALOG_JHOIRA = 4;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case DIALOG_RULES: {
                        /* Use a generic AlertDialog to display the rules text */
                MaterialDialog.Builder builder = new MaterialDialog.Builder(this.getActivity());
                builder.neutralText(R.string.mojhosto_dialog_play)
                        .content(ImageGetterHelper.formatHtmlString(getString(R.string.mojhosto_rules_text)))
                        .title(R.string.mojhosto_rules_title);
                return builder.build();
            }
            case DIALOG_MOMIR:
            case DIALOG_STONEHEWER:
            case DIALOG_JHOIRA: {
                        /* Use a raw dialog with a custom view (ImageView inside LinearLayout) to display the Vanguard*/
                Dialog dialog = new Dialog(this.getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.card_view_image_dialog);
                ImageView image = (ImageView) dialog.findViewById(R.id.cardimage);

                        /* These drawables are re-sized on-the-fly, so only a single hi-res version exists in a resource
                           folder without density */
                switch (mDialogId) {
                    case DIALOG_MOMIR:
                        image.setImageResource(R.drawable.mjs_momir);
                        break;
                    case DIALOG_STONEHEWER:
                        image.setImageResource(R.drawable.mjs_stonehewer);
                        break;
                    case DIALOG_JHOIRA:
                        image.setImageResource(R.drawable.mjs_jhoira);
                        break;
                }

                        /* Make a DP border */
                int border = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());

                        /* Get the screen size in px */
                Rect rectangle = new Rect();
                Window window = getActivity().getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                int windowHeight = rectangle.height();
                int windowWidth = rectangle.width();

                        /* Get the drawable size in px */
                assert image.getDrawable() != null;
                int imageHeight = image.getDrawable().getIntrinsicHeight();
                int imageWidth = image.getDrawable().getIntrinsicWidth();

                        /* Figure out how much to scale the drawable */
                float scaleFactor;
                if ((imageHeight / (float) imageWidth) > (windowHeight / (float) windowWidth)) {
                            /* Limiting factor is height */
                    scaleFactor = (windowHeight - border) / (float) imageHeight;
                } else {
                            /* Limiting factor is width */
                    scaleFactor = (windowWidth - border) / (float) imageWidth;
                }

                        /* Scale the drawable */
                image.setLayoutParams(new LinearLayout.LayoutParams((int) (imageWidth * scaleFactor),
                        (int) (imageHeight * scaleFactor)));

                return dialog;
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}
