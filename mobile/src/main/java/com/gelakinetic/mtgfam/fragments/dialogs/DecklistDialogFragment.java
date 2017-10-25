package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Class that creates dialogs for DecklistFragment.
 */
public class DecklistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_SAVE_DECK_AS = 2;
    public static final int DIALOG_LOAD_DECK = 3;
    public static final int DIALOG_DELETE_DECK = 4;
    public static final int DIALOG_CONFIRMATION = 5;

    public static final String NAME_KEY = "name";
    public static final String SIDE_KEY = "side";

    /**
     * @return The currently viewed DecklistFragment
     */
    @Nullable
    private DecklistFragment getParentDecklistFragment() {
        return (DecklistFragment) getParentFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setShowsDialog(true);
        mDialogId = getArguments().getInt(ID_KEY);
        final String cardName = getArguments().getString(NAME_KEY);
        final boolean isSideboard = getArguments().getBoolean(SIDE_KEY);

        if (null == getParentDecklistFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                Dialog dialog = CardHelpers.getDialog(
                        cardName,
                        getParentDecklistFragment(),
                        true,
                        isSideboard
                );
                if (dialog == null) {
                    if (null != getParentDecklistFragment()) {
                        getParentDecklistFragment().handleFamiliarDbException(false);
                    }
                    return DontShowDialog();
                }
                return dialog;
            }
            case DIALOG_SAVE_DECK_AS: {
                /* Inflate a view to type in the deck's name and show it in an AlertDialog */
                View textEntryView = getActivity().getLayoutInflater()
                        .inflate(R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                nameInput.append(getParentDecklistFragment().mCurrentDeck);
                /* Set the button to clear the text field */
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                nameInput.setText("");
                            }

                        });
                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.decklist_save)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {

                            @Override
                            public void onClick(
                                    @NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {

                                if (nameInput.getText() == null) {
                                    return;
                                }
                                String deckName = nameInput.getText().toString();
                                /* Don't save if there is not a name */
                                if (deckName.length() == 0 || deckName.equals("")) {
                                    return;
                                }
                                DecklistHelpers.WriteCompressedDecklist(
                                        getActivity(),
                                        getParentDecklistFragment().mCompressedDecklist,
                                        deckName + DecklistFragment.DECK_EXTENSION
                                );
                                getParentDecklistFragment().mCurrentDeck = deckName;
                                getParentDecklistFragment().mDeckName.setText(deckName);

                            }

                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow()
                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_LOAD_DECK: {

                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* If there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {

                            @Override
                            public void onSelection(
                                    MaterialDialog dialog,
                                    View itemView,
                                    int position,
                                    CharSequence text) {

                                getParentDecklistFragment()
                                        .readAndCompressDecklist(null, deckNames[position]);
                                getParentDecklistFragment().mCurrentDeck = deckNames[position];
                                /* Alert things to update */
                                getParentDecklistFragment().mListAdapter.notifyDataSetChanged();

                            }

                        })
                        .build();
            }
            case DIALOG_DELETE_DECK: {

                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* if there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    ToastWrapper.makeText(
                            this.getActivity(),
                            R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG
                    ).show();
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_delete_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(
                                    MaterialDialog dialog,
                                    View itemView,
                                    int position,
                                    CharSequence text) {

                                File toDelete = new File(getActivity().getFilesDir(),
                                        deckNames[position] + DecklistFragment.DECK_EXTENSION);
                                if (!toDelete.delete()) {
                                    ToastWrapper.makeText(
                                            getActivity(),
                                            toDelete.getName() + " "
                                                    + getString(R.string.not_deleted),
                                            ToastWrapper.LENGTH_LONG
                                    ).show();
                                }

                            }

                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_clear)
                        .content(R.string.decklist_clear_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {

                            @Override
                            public void onClick(
                                    @NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {

                                /* do some cleaning up */
                                getParentDecklistFragment().mCurrentDeck = "autosave";
                                getParentDecklistFragment().mCompressedDecklist.clear();
                                getParentDecklistFragment().mListAdapter.notifyDataSetChanged();
                                getParentDecklistFragment().mDeckName.setText(
                                        R.string.decklist_unnamed_deck
                                );
                                getParentDecklistFragment().mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count, 0, 0));
                                DecklistHelpers.WriteCompressedDecklist(
                                        getActivity(),
                                        getParentDecklistFragment().mCompressedDecklist
                                );
                                dialog.dismiss();

                            }

                        })
                        .negativeText(R.string.dialog_cancel)
                        .cancelable(true)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

}
