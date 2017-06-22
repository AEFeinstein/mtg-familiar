package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.Language;
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
    public static final int SHARE_CARD = 8;
    public static final int TRANSLATE_CARD = 9;

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
                        ((CardViewFragment.saveCardImageTask) getCardViewFragment().mAsyncTask).execute(CardViewFragment.MAIN_PAGE);
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

                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                builder.customView(lv, false);
                builder.title(R.string.card_view_legality);
                return builder.build();
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

                MaterialDialog.Builder adb = new MaterialDialog.Builder(getActivity());
                adb.customView(v, false);
                adb.title(R.string.card_view_price_dialog_title);
                return adb.build();
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
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                builder.title(R.string.card_view_set_dialog_title);
                builder.items(aSets);
                builder.itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        getCardViewFragment().setInfoFromID(aIds[position]);
                    }
                });
                return builder.build();
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

                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                builder.title(R.string.card_view_rulings_dialog_title);
                builder.customView(v, false);
                return builder.build();
            }
            case WISH_LIST_COUNTS: {
                Dialog dialog = WishlistHelpers.getDialog(getCardViewFragment().mCardName, getCardViewFragment(), false);
                if (dialog == null) {
                    getCardViewFragment().handleFamiliarDbException(false);
                    return DontShowDialog();
                }
                return dialog;
            }
            case SHARE_CARD: {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                        .title(R.string.card_view_share_card)
                        .positiveText(R.string.card_view_share_text)
                        //.negativeText(R.string.card_view_share_image) // haven't gotten this to work yet
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                View view = getCardViewFragment().getView();
                                SpannableString costSpannable = new SpannableString(((TextView) view.findViewById(R.id.cost)).getText());
                                SpannableString abilitySpannable = new SpannableString(((TextView) view.findViewById(R.id.ability)).getText());
                                String costText = getCardViewFragment().convertHtmlToPlainText(Html.toHtml(costSpannable));
                                String abilityText = getCardViewFragment().convertHtmlToPlainText(Html.toHtml(abilitySpannable));
                                String copyText = ((TextView) view.findViewById(R.id.name)).getText().toString() + '\n' +
                                        costText + '\n' +
                                        ((TextView) view.findViewById(R.id.type)).getText().toString() + '\n' +
                                        ((TextView) view.findViewById(R.id.set)).getText().toString() + '\n' +
                                        abilityText + '\n' +
                                        ((TextView) view.findViewById(R.id.flavor)).getText().toString() + '\n' +
                                        ((TextView) view.findViewById(R.id.pt)).getText().toString() + '\n' +
                                        ((TextView) view.findViewById(R.id.artist)).getText().toString() + '\n' +
                                        ((TextView) view.findViewById(R.id.number)).getText().toString();
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, copyText);
                                sendIntent.setType("text/plain");
                                startActivity(sendIntent);
                            }
                        })
                        .negativeText(R.string.card_view_image)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                getCardViewFragment().runShareImageTask();
                            }
                        });
                return builder.build();
            }
            case TRANSLATE_CARD: {
                /* Make sure the translations exist */
                if (getCardViewFragment().mTranslatedNames == null || getCardViewFragment().mTranslatedNames.isEmpty()) {
                    /* exception handled in AsyncTask */
                    return DontShowDialog();
                }

                /* create the item mapping */
                String[] from = new String[]{"lang", "name"};
                int[] to = new int[]{R.id.format, R.id.status};

                /* prepare the list of all translations */
                List<HashMap<String, String>> fillMaps = new ArrayList<>();
                for (Card.ForeignPrinting fp : getCardViewFragment().mTranslatedNames) {
                    HashMap<String, String> map = new HashMap<>();

                    /* Translate the language code into a readable language label */
                    String language = null;
                    switch (fp.mLanguageCode) {
                        case Language.Chinese_Traditional: {
                            language = getString(R.string.pref_Chinese_trad);
                            break;
                        }
                        case Language.Chinese_Simplified: {
                            language = getString(R.string.pref_Chinese);
                            break;
                        }
                        case Language.French: {
                            language = getString(R.string.pref_French);
                            break;
                        }
                        case Language.German: {
                            language = getString(R.string.pref_German);
                            break;
                        }
                        case Language.Italian: {
                            language = getString(R.string.pref_Italian);
                            break;
                        }
                        case Language.Japanese: {
                            language = getString(R.string.pref_Japanese);
                            break;
                        }
                        case Language.Portuguese_Brazil: {
                            language = getString(R.string.pref_Portuguese);
                            break;
                        }
                        case Language.Russian: {
                            language = getString(R.string.pref_Russian);
                            break;
                        }
                        case Language.Spanish: {
                            language = getString(R.string.pref_Spanish);
                            break;
                        }
                        case Language.Korean: {
                            language = getString(R.string.pref_Korean);
                            break;
                        }
                        case Language.English: {
                            language = getString(R.string.pref_English);
                            break;
                        }
                    }

                    /* Add the language and translation */
                    map.put(from[0], language);
                    map.put(from[1], fp.mName);
                    fillMaps.add(map);
                }

                SimpleAdapter adapter = new SimpleAdapter(getActivity(), fillMaps, R.layout.card_view_legal_row,
                        from, to);
                ListView lv = new ListView(getActivity());
                lv.setAdapter(adapter);
                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        /* Copy the translated name to the clipboard */
                        ClipboardManager clipboard = (ClipboardManager) (getCardViewFragment().getContext().
                                getSystemService(android.content.Context.CLIPBOARD_SERVICE));
                        ClipData cd = new ClipData(
                                ((TextView) view.findViewById(R.id.format)).getText(),
                                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                new ClipData.Item(((TextView) view.findViewById(R.id.status)).getText()));
                        clipboard.setPrimaryClip(cd);

                        Toast.makeText(getContext(), R.string.card_view_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });

                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                builder.customView(lv, false);
                builder.title(R.string.card_view_translated_dialog_title);
                return builder.build();
            }
            default: {
                return DontShowDialog();
            }
        }
    }
}