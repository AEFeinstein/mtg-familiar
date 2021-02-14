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

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

import java.util.Objects;

/**
 * Class that creates dialogs for MoJhoStoFragment
 */
public class MoJhoStoDialogFragment extends FamiliarDialogFragment {

    /* Dialog Constants */
    public static final int DIALOG_RULES = 1;
    public static final int DIALOG_MOMIR = 2;
    public static final int DIALOG_STONEHEWER = 3;
    public static final int DIALOG_JHOIRA = 4;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);
        switch (mDialogId) {
            case DIALOG_RULES: {
                /* Use a generic AlertDialog to display the rules text */
                MaterialDialog.Builder builder = new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()));
                builder.neutralText(R.string.mojhosto_dialog_play)
                        .content(ImageGetterHelper.formatHtmlString(getString(R.string.mojhosto_rules_text)))
                        .title(R.string.mojhosto_rules_title);
                return builder.build();
            }
            case DIALOG_MOMIR:
            case DIALOG_STONEHEWER:
            case DIALOG_JHOIRA: {
                /* Use a raw dialog with a custom view (ImageView inside LinearLayout) to display the Vanguard*/
                Dialog dialog = new Dialog(Objects.requireNonNull(this.getActivity()));
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.card_view_image_dialog);
                ImageView image = dialog.findViewById(R.id.cardimage);

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
                ViewGroup.LayoutParams params = image.getLayoutParams();
                if (null != params) {
                    params.width = (int) (imageWidth * scaleFactor);
                    params.height = (int) (imageHeight * scaleFactor);
                    image.setLayoutParams(params);
                }

                return dialog;
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}
