package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Class that creates dialogs for CardViewFragment
 */
public class CardViewDialogFragment extends FamiliarDialogFragment {

    /* Dialogs */
    public static final int GET_PRICE = 1;
    public static final int GET_IMAGE = 2;
    public static final int CHANGE_SET = 3;
    public static final int CARD_RULINGS = 4;
    public static final int WISH_LIST_COUNTS = 6;
    public static final int GET_LEGALITY = 7;

    /**
     * @return the currently viewed CardViewFragment in the CardViewPagerFragment
     */
    private CardViewFragment getCardViewFragment() {
        return ((CardViewPagerFragment) getFamiliarFragment()).getCurrentFragment();
    }

    @NotNull
    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case GET_IMAGE: {
                if (getCardViewFragment().mCardBitmap == null) {
                    return DontShowDialog();
                }

                Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                dialog.setContentView(R.layout.card_view_image_dialog);

                ImageView dialogImageView = (ImageView) dialog.findViewById(R.id.cardimage);
                dialogImageView.setImageDrawable(getCardViewFragment().mCardBitmap);

                dialogImageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (getCardViewFragment().mAsyncTask != null) {
                            getCardViewFragment().mAsyncTask.cancel(true);
                        }
                        getCardViewFragment().mAsyncTask = getCardViewFragment().new saveCardImageTask();
                        getCardViewFragment().mAsyncTask.execute((Void[]) null);
                        return true;
                    }
                });

                return dialog;
            }
            case GET_LEGALITY: {
                if (getCardViewFragment().mFormats == null || getCardViewFragment().mLegalities == null) {
                            /* exception handled in AsyncTask */
                    return DontShowDialog();
                }

                        /* create the item mapping */
                String[] from = new String[]{"format", "status"};
                int[] to = new int[]{R.id.format, R.id.status};

                        /* prepare the list of all records */
                List<HashMap<String, String>> fillMaps = new ArrayList<>();
                for (int i = 0; i < getCardViewFragment().mFormats.length; i++) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(from[0], getCardViewFragment().mFormats[i]);
                    map.put(from[1], getCardViewFragment().mLegalities[i]);
                    fillMaps.add(map);
                }

                SimpleAdapter adapter = new SimpleAdapter(getActivity(), fillMaps, R.layout.card_view_legal_row,
                        from, to);
                ListView lv = new ListView(getActivity());
                lv.setAdapter(adapter);

                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
                builder.setView(lv);
                builder.setTitle(R.string.card_view_legality);
                return builder.create();
            }
            case GET_PRICE: {
                if (getCardViewFragment().mPriceInfo == null) {
                    return DontShowDialog();
                }

                View v = getActivity().getLayoutInflater().inflate(R.layout.card_view_price_dialog, null, false);

                assert v != null; /* Because Android Studio */
                TextView l = (TextView) v.findViewById(R.id.low);
                TextView m = (TextView) v.findViewById(R.id.med);
                TextView h = (TextView) v.findViewById(R.id.high);
                TextView f = (TextView) v.findViewById(R.id.foil);
                TextView priceLink = (TextView) v.findViewById(R.id.pricelink);

                l.setText(String.format(Locale.US, "$%1$,.2f", getCardViewFragment().mPriceInfo.mLow));
                m.setText(String.format(Locale.US, "$%1$,.2f", getCardViewFragment().mPriceInfo.mAverage));
                h.setText(String.format(Locale.US, "$%1$,.2f", getCardViewFragment().mPriceInfo.mHigh));

                if (getCardViewFragment().mPriceInfo.mFoilAverage != 0) {
                    f.setText(String.format(Locale.US, "$%1$,.2f", getCardViewFragment().mPriceInfo.mFoilAverage));
                } else {
                    f.setVisibility(View.GONE);
                    v.findViewById(R.id.foil_label).setVisibility(View.GONE);
                }
                priceLink.setMovementMethod(LinkMovementMethod.getInstance());
                priceLink.setText(ImageGetterHelper.formatHtmlString("<a href=\"" + getCardViewFragment().mPriceInfo.mUrl + "\">" +
                        getString(R.string.card_view_price_dialog_link) + "</a>"));

                AlertDialogWrapper.Builder adb = new AlertDialogWrapper.Builder(getActivity());
                adb.setView(v);
                adb.setTitle(R.string.card_view_price_dialog_title);
                return adb.create();
            }
            case CHANGE_SET: {
                final String[] aSets = getCardViewFragment().mPrintings.toArray(new String[getCardViewFragment().mPrintings.size()]);
                final Long[] aIds = getCardViewFragment().mCardIds.toArray(new Long[getCardViewFragment().mCardIds.size()]);

                        /* Sanity check */
                for (String set : aSets) {
                    if (set == null) {
                        return DontShowDialog();
                    }
                }
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
                builder.setTitle(R.string.card_view_set_dialog_title);
                builder.setItems(aSets, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int item) {
                        getCardViewFragment().setInfoFromID(aIds[item]);
                    }
                });
                return builder.create();
            }
            case CARD_RULINGS: {
                if (getCardViewFragment().mRulingsArrayList == null) {
                    return DontShowDialog();
                }
                Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getActivity());

                View v = getActivity().getLayoutInflater().inflate(R.layout.card_view_rulings_dialog, null, false);
                assert v != null; /* Because Android Studio */

                TextView textViewRules = (TextView) v.findViewById(R.id.rules);
                TextView textViewUrl = (TextView) v.findViewById(R.id.url);

                String message = "";
                if (getCardViewFragment().mRulingsArrayList.size() == 0) {
                    message = getString(R.string.card_view_no_rulings);
                } else {
                    for (CardViewFragment.Ruling r : getCardViewFragment().mRulingsArrayList) {
                        message += (r.toString() + "<br><br>");
                    }

                    message = message.replace("{Tap}", "{T}");
                }
                CharSequence messageGlyph = ImageGetterHelper.formatStringWithGlyphs(message, imgGetter);

                textViewRules.setText(messageGlyph);

                textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
                textViewUrl.setText(Html.fromHtml(
                        "<a href=http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" +
                                getCardViewFragment().mMultiverseId + ">" + getString(R.string.card_view_gatherer_page) + "</a>"
                ));

                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
                builder.setTitle(R.string.card_view_rulings_dialog_title);
                builder.setView(v);
                return builder.create();
            }
            case WISH_LIST_COUNTS: {
                Dialog dialog = WishlistHelpers.getDialog(getCardViewFragment().mCardName, getCardViewFragment(), false);
                if (dialog == null) {
                    getCardViewFragment().handleFamiliarDbException(false);
                    return DontShowDialog();
                }
                return dialog;
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}