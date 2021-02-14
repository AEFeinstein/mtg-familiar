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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.Language;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.ExpansionImageHelper;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    public static final int ADD_TO_DECKLIST = 10;

    /**
     * @return the currently viewed CardViewFragment in the CardViewPagerFragment
     */
    @Nullable
    private CardViewFragment getParentCardViewFragment() {
        try {
            CardViewPagerFragment pagerFrag = ((CardViewPagerFragment) getParentFamiliarFragment());
            if (null != pagerFrag) {
                return pagerFrag.getCurrentFragment();
            }
        } catch (ClassCastException e) {
            return null;
        }
        return null;
    }

    @NonNull
    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);

        if (null == getParentCardViewFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case GET_IMAGE: {

                Dialog dialog = new Dialog(getParentCardViewFragment().mActivity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.card_view_image_dialog);
                ImageView dialogImageView = dialog.findViewById(R.id.cardimage);

                // Set the image loaded with Glide
                dialogImageView.setImageDrawable(getParentCardViewFragment().getImageDrawable());

                dialogImageView.setOnLongClickListener(view -> {
                    getParentCardViewFragment().saveImageWithGlide(CardViewFragment.MAIN_PAGE);
                    return true;
                });

                return dialog;
            }
            case GET_LEGALITY: {
                if (null == getParentCardViewFragment() || getParentCardViewFragment().mFormats == null || getParentCardViewFragment().mLegalities == null) {
                    /* exception handled in AsyncTask */
                    return DontShowDialog();
                }

                /* create the item mapping */
                String[] from = new String[]{"format", "status"};
                int[] to = new int[]{R.id.format, R.id.status};

                /* prepare the list of all records */
                List<HashMap<String, String>> fillMaps = new ArrayList<>();
                for (int i = 0; i < getParentCardViewFragment().mFormats.length; i++) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(from[0], getParentCardViewFragment().mFormats[i]);
                    map.put(from[1], getParentCardViewFragment().mLegalities[i]);
                    fillMaps.add(map);
                }

                SimpleAdapter adapter = new SimpleAdapter(getParentCardViewFragment().mActivity, fillMaps, R.layout.card_view_legal_row,
                        from, to);
                ListView lv = new ListView(getParentCardViewFragment().mActivity);
                lv.setAdapter(adapter);

                MaterialDialog.Builder builder = new MaterialDialog.Builder(getParentCardViewFragment().mActivity);
                builder.customView(lv, false);
                builder.title(R.string.card_view_legality);
                return builder.build();
            }
            case GET_PRICE: {
                if (null == getParentCardViewFragment() || getParentCardViewFragment().mPriceInfo == null) {
                    return DontShowDialog();
                }

                @SuppressLint("InflateParams") View v = getParentCardViewFragment().mActivity.getLayoutInflater().inflate(R.layout.card_view_price_dialog, null, false);

                assert v != null; /* Because Android Studio */

                final String priceFormat = "$%1$,.2f";
                MarketPriceInfo price = getParentCardViewFragment().mPriceInfo;
                if (null != price) {
                    if (price.hasNormalPrice()) {
                        ((TextView) v.findViewById(R.id.normal_low)).setText(String.format(Locale.US, priceFormat, price.getPrice(false, MarketPriceInfo.PriceType.LOW).price));
                        ((TextView) v.findViewById(R.id.normal_mid)).setText(String.format(Locale.US, priceFormat, price.getPrice(false, MarketPriceInfo.PriceType.MID).price));
                        ((TextView) v.findViewById(R.id.normal_high)).setText(String.format(Locale.US, priceFormat, price.getPrice(false, MarketPriceInfo.PriceType.HIGH).price));
                        ((TextView) v.findViewById(R.id.normal_market)).setText(String.format(Locale.US, priceFormat, price.getPrice(false, MarketPriceInfo.PriceType.MARKET).price));
                    } else {
                        v.findViewById(R.id.normal_prices).setVisibility(View.GONE);
                        v.findViewById(R.id.normal_foil_divider).setVisibility(View.GONE);
                    }

                    if (price.hasFoilPrice()) {
                        ((TextView) v.findViewById(R.id.foil_low)).setText(String.format(Locale.US, priceFormat, price.getPrice(true, MarketPriceInfo.PriceType.LOW).price));
                        ((TextView) v.findViewById(R.id.foil_mid)).setText(String.format(Locale.US, priceFormat, price.getPrice(true, MarketPriceInfo.PriceType.MID).price));
                        ((TextView) v.findViewById(R.id.foil_high)).setText(String.format(Locale.US, priceFormat, price.getPrice(true, MarketPriceInfo.PriceType.HIGH).price));
                        ((TextView) v.findViewById(R.id.foil_market)).setText(String.format(Locale.US, priceFormat, price.getPrice(true, MarketPriceInfo.PriceType.MARKET).price));
                    } else {
                        v.findViewById(R.id.foil_prices).setVisibility(View.GONE);
                        v.findViewById(R.id.normal_foil_divider).setVisibility(View.GONE);
                    }

                    TextView priceLink = v.findViewById(R.id.pricelink);
                    priceLink.setMovementMethod(LinkMovementMethod.getInstance());
                    priceLink.setText(ImageGetterHelper.formatHtmlString("<a href=\"" + getParentCardViewFragment().mPriceInfo.getUrl() + "\">" +
                            getString(R.string.card_view_price_dialog_link) + "</a>"));

                    MaterialDialog.Builder adb = new MaterialDialog.Builder(getParentCardViewFragment().mActivity);
                    adb.customView(v, false);
                    adb.title(R.string.card_view_price_dialog_title);
                    return adb.build();
                } else {
                    return DontShowDialog();
                }
            }
            case CHANGE_SET: {

                /* Sanity check */
                for (ExpansionImageHelper.ExpansionImageData set : getParentCardViewFragment().mPrintings) {
                    if (set == null) {
                        return DontShowDialog();
                    }
                }

                /* Build and return the dialog */
                ExpansionImageHelper.ChangeSetListAdapter adapter = (new ExpansionImageHelper()).
                        new ChangeSetListAdapter(getContext(), getParentCardViewFragment().mPrintings, ExpansionImageHelper.ExpansionImageSize.LARGE) {
                    @Override
                    protected void onClick(ExpansionImageHelper.ExpansionImageData data) {
                        getParentCardViewFragment().setInfoFromID(data.getDbId());
                    }
                };
                Dialog dialog = new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                        .title(R.string.card_view_set_dialog_title)
                        .adapter(adapter, null)
                        .build();
                adapter.setDialogReference(dialog);
                return dialog;
            }
            case CARD_RULINGS: {
                if (null == getParentCardViewFragment() || getParentCardViewFragment().mRulingsArrayList == null) {
                    return DontShowDialog();
                }
                Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(getParentCardViewFragment().mActivity);

                @SuppressLint("InflateParams") View v = getParentCardViewFragment().mActivity.getLayoutInflater().inflate(R.layout.card_view_rulings_dialog, null, false);
                assert v != null; /* Because Android Studio */

                TextView textViewRules = v.findViewById(R.id.rules);
                TextView textViewUrl = v.findViewById(R.id.url);

                String message;
                if (getParentCardViewFragment().mRulingsArrayList.size() == 0) {
                    message = getString(R.string.card_view_no_rulings);
                } else {
                    StringBuilder messageBuilder = new StringBuilder();
                    for (CardViewFragment.Ruling r : getParentCardViewFragment().mRulingsArrayList) {
                        messageBuilder.append(r.toString()).append("<br><br>");
                    }

                    message = messageBuilder.toString().replace("{Tap}", "{T}");
                }
                CharSequence messageGlyph = ImageGetterHelper.formatStringWithGlyphs(message, imgGetter);

                textViewRules.setText(messageGlyph);

                textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
                // Gatherer doesn't use HTTPS as of 1/6/2019
                textViewUrl.setText(Html.fromHtml(
                        "<a href=https://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" +
                                getParentCardViewFragment().mCard.getMultiverseId() + ">" + getString(R.string.card_view_gatherer_page) + "</a>"
                ));

                MaterialDialog.Builder builder = new MaterialDialog.Builder(getParentCardViewFragment().mActivity);
                builder.title(R.string.card_view_rulings);
                builder.customView(v, false);
                return builder.build();
            }
            case WISH_LIST_COUNTS: {
                if (null == getParentCardViewFragment()) {
                    return DontShowDialog();
                }
                Dialog dialog = CardHelpers.getDialog(getParentCardViewFragment().mCard.getName(), getParentCardViewFragment(), false, false);
                if (dialog == null) {
                    getParentCardViewFragment().handleFamiliarDbException(false);
                    return DontShowDialog();
                }
                return dialog;
            }
            case ADD_TO_DECKLIST: {
                if (null == getParentCardViewFragment()) {
                    return DontShowDialog();
                }

                final String cardName = getParentCardViewFragment().mCard.getName();
                final String cardSet = getParentCardViewFragment().mCard.getExpansion();
                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* If there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    SnackbarWrapper.makeAndShowText(this.getParentCardViewFragment().mActivity, R.string.decklist_toast_no_decks,
                            SnackbarWrapper.LENGTH_LONG);
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getParentCardViewFragment().mActivity)
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback((dialog, itemView, position, text) -> {

                            try {
                                // Read the decklist
                                String deckFileName = deckNames[position] + DecklistFragment.DECK_EXTENSION;
                                ArrayList<MtgCard> decklist =
                                        DecklistHelpers.ReadDecklist(getActivity(), deckFileName, false);

                                // Look through the decklist for any existing matches
                                boolean entryIncremented = false;
                                for (MtgCard deckEntry : decklist) {
                                    if (!deckEntry.isSideboard() && // not in the sideboard
                                            deckEntry.getName().equals(cardName) &&
                                            deckEntry.getExpansion().equals(cardSet)) {
                                        // Increment the card already in the deck
                                        deckEntry.mNumberOf++;
                                        entryIncremented = true;
                                        break;
                                    }
                                }
                                if (!entryIncremented) {
                                    // Add a new card to the deck
                                    decklist.add(new MtgCard(cardName, cardSet, false, 1, false));
                                }

                                // Write the decklist back
                                DecklistHelpers.WriteDecklist(Objects.requireNonNull(getActivity()), decklist, deckFileName);
                            } catch (FamiliarDbException e) {
                                getParentCardViewFragment().handleFamiliarDbException(false);
                            }
                        })
                        .build();
            }
            case SHARE_CARD: {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getParentCardViewFragment().mActivity)
                        .title(R.string.card_view_share_card)
                        .positiveText(R.string.search_text)
                        .onPositive((dialog, which) -> {
                            View view = getParentCardViewFragment().getView();
                            if (view == null) {
                                return;
                            }
                            SpannableString costSpannable = new SpannableString(((TextView) view.findViewById(R.id.cost)).getText());
                            SpannableString abilitySpannable = new SpannableString(((TextView) view.findViewById(R.id.ability)).getText());
                            String costText = getParentCardViewFragment().convertHtmlToPlainText(Html.toHtml(costSpannable));
                            String abilityText = getParentCardViewFragment().convertHtmlToPlainText(Html.toHtml(abilitySpannable));
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
                        })
                        .negativeText(R.string.card_view_image)
                        .onNegative((dialog, which) -> getParentCardViewFragment().saveImageWithGlide(CardViewFragment.SHARE));
                return builder.build();
            }
            case TRANSLATE_CARD: {
                /* Make sure the translations exist */
                if (null == getParentCardViewFragment() || getParentCardViewFragment().mCard.getForeignPrintings().isEmpty()) {
                    /* exception handled in AsyncTask */
                    return DontShowDialog();
                }

                /* create the item mapping */
                String[] from = new String[]{"lang", "name"};
                int[] to = new int[]{R.id.format, R.id.status};

                /* prepare the list of all translations */
                List<HashMap<String, String>> fillMaps = new ArrayList<>();
                for (Card.ForeignPrinting fp : getParentCardViewFragment().mCard.getForeignPrintings()) {
                    HashMap<String, String> map = new HashMap<>();

                    /* Translate the language code into a readable language label */
                    String language = null;
                    switch (fp.getLanguageCode()) {
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
                    map.put(from[1], fp.getName());
                    fillMaps.add(map);
                }

                SimpleAdapter adapter = new SimpleAdapter(getParentCardViewFragment().mActivity, fillMaps, R.layout.card_view_legal_row,
                        from, to);
                ListView lv = new ListView(getParentCardViewFragment().mActivity);
                lv.setAdapter(adapter);
                lv.setOnItemLongClickListener((parent, view, position, id) -> {
                    /* Copy the translated name to the clipboard */
                    ClipboardManager clipboard = (ClipboardManager) (Objects.requireNonNull(getParentCardViewFragment().getContext()).
                            getSystemService(android.content.Context.CLIPBOARD_SERVICE));
                    if (null != clipboard) {
                        ClipData cd = new ClipData(
                                ((TextView) view.findViewById(R.id.format)).getText(),
                                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                                new ClipData.Item(((TextView) view.findViewById(R.id.status)).getText()));
                        clipboard.setPrimaryClip(cd);

                        SnackbarWrapper.makeAndShowText(getParentCardViewFragment().mActivity, R.string.card_view_copied_to_clipboard, SnackbarWrapper.LENGTH_SHORT);
                    }
                    return false;
                });

                MaterialDialog.Builder builder = new MaterialDialog.Builder(Objects.requireNonNull(getParentCardViewFragment()).mActivity);
                builder.customView(lv, false);
                builder.title(R.string.card_view_translated_dialog_title);
                return builder.build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}