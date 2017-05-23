package com.gelakinetic.mtgfam.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.DecklistDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DecklistFragment extends FamiliarFragment {

    /* UI Elements */
    private AutoCompleteTextView mNameField;
    private EditText mNumberField;
    public TextView mDeckName;
    private TextView mDeckCards;
    private TextView mDeckPrice;

    /* Decklist and adapters */
    private RecyclerView decklistView;
    public ArrayList<CompressedDecklistInfo> mCompressedDecklist;
    public CardDataAdapter mDecklistAdapter;
    private ComparatorChain<CompressedDecklistInfo> mDecklistChain;

    public static final String AUTOSAVE_NAME = "autosave";
    public String mCurrentDeck = "";
    public static final String DECK_EXTENSION = ".deck";

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to be displayed
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myFragmentView = inflater.inflate(R.layout.decklist_frag, container, false);
        assert myFragmentView != null;

        final TextView.OnEditorActionListener addCardListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    addCardToDeck(false);
                    return true;
                }
                return false;
            }
        };

        /* set the autocomplete for card names */
        mNameField = (AutoCompleteTextView) myFragmentView.findViewById(R.id.name_search);
        mNameField.setAdapter(new AutocompleteCursorAdapter(this, new String[]{CardDbAdapter.KEY_NAME}, new int[]{R.id.text1}, mNameField, false));
        mNameField.setOnEditorActionListener(addCardListener);

        /* Default the number of cards field */
        mNumberField = (EditText) myFragmentView.findViewById(R.id.number_input);
        mNumberField.setText("1");
        mNumberField.setOnEditorActionListener(addCardListener);

        decklistView = (RecyclerView) myFragmentView.findViewById(R.id.decklist);
        decklistView.setLayoutManager(new LinearLayoutManager(getContext()));

        myFragmentView.findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCardToDeck(false);
            }
        });
        myFragmentView.findViewById(R.id.add_card).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                addCardToDeck(true);
                return true;
            }
        });

        /* Set up the decklist and adapter, it will be read in onResume() */
        mCompressedDecklist = new ArrayList<>();
        mDecklistAdapter = new CardDataAdapter(mCompressedDecklist);
        decklistView.setAdapter(mDecklistAdapter);

        /* Decklist information */
        mDeckName = (TextView) myFragmentView.findViewById(R.id.decklistName);
        mDeckName.setText(R.string.decklist_unnamed_deck);
        mDeckCards = (TextView) myFragmentView.findViewById(R.id.decklistCards);
        mDeckCards.setText("0 ");
        mDeckPrice = (TextView) myFragmentView.findViewById(R.id.decklistPrice);
        mDeckPrice.setVisibility(View.GONE);

        mDecklistChain = new ComparatorChain<>();
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSideboard());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorSupertype(getResources().getStringArray(R.array.card_types_extra)));
        mDecklistChain.addComparator(new CardHelpers.CardComparatorCMC());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorColor());
        mDecklistChain.addComparator(new CardHelpers.CardComparatorName());

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                xMark = ContextCompat.getDrawable(getContext(), R.drawable.ic_menu_delete_light);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = 10; // todo: actually make this dimension
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int swipedPosition = viewHolder.getAdapterPosition();
                mDecklistAdapter.pendingRemoval(swipedPosition);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyAction) {
                View itemView = viewHolder.itemView;
                if (viewHolder.getAdapterPosition() == -1) {
                    return;
                }
                if (!initiated) {
                    init();
                }
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = xMark.getIntrinsicWidth();
                int intrinsicHeight = xMark.getIntrinsicHeight();
                int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - xMarkMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
                xMark.draw(c);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyAction);
            }

            @Override
            public int getSwipeDirs(RecyclerView parent, RecyclerView.ViewHolder holder) {
                if (holder instanceof CardDataAdapter.ViewHolder) {
                    if (!((CardDataAdapter.ViewHolder) holder).swipeable) {
                        return 0;
                    }
                }
                return super.getSwipeDirs(parent, holder);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(decklistView);
        decklistView.addItemDecoration(new RecyclerView.ItemDecoration() {
            Drawable background;
            boolean initiated;
            private void init() {
                background = new ColorDrawable(Color.RED);
                initiated = true;
            }
            @Override
            public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
                if (!initiated) {
                    init();
                }
                if (parent.getItemAnimator().isRunning()) {
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;
                    int left = 0;
                    int right = parent.getWidth();
                    int top = 0;
                    int bottom = 0;
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (child.getTranslationY() < 0) {
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }
                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }
                    background.setBounds(left, top, right, bottom);
                    background.draw(canvas);
                }
                super.onDraw(canvas, parent, state);
            }
        });

        mDecklistAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = decklistView.indexOfChild(view);
                CompressedDecklistInfo item = mCompressedDecklist.get(position);
                /* Show the dialog for this particular card */
                showDialog(DecklistDialogFragment.DIALOG_UPDATE_CARD, item.mCard.mName, item.mIsSideboard);
            }
        });

        return myFragmentView;
    }

    /**
     * This function takes care of adding a card to the decklist from this fragment. It makes sure
     * that fields are not null or have bad information.
     * @param isSideboard if the card is in the sideboard
     */
    private void addCardToDeck(boolean isSideboard) {
        String name = String.valueOf(mNameField.getText());
        String numberOf = String.valueOf(mNumberField.getText());
        /* Don't allow the fields to be empty */
        if (name == null || name.equals("")) {
            return;
        }
        if (numberOf == null || numberOf.equals("")) {
            return;
        }
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            /* Make the new card */
            MtgCard card = new MtgCard();
            card.mName = name;
            card.foil = false;
            card.numberOf = Integer.parseInt(numberOf);
            card.message = getString(R.string.wishlist_loading);

            /* Get some extra information from the database */
            Cursor cardCursor = CardDbAdapter.fetchCardByName(card.mName, CardDbAdapter.allCardDataKeys, true, database);
            if (cardCursor.getCount() == 0) {
                ToastWrapper.makeText(DecklistFragment.this.getActivity(), getString(R.string.toast_no_card),
                        ToastWrapper.LENGTH_LONG).show();
                DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                return;
            }
            card.mName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            card.mType = CardDbAdapter.getTypeLine(cardCursor);
            card.mRarity = (char) cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
            card.mManaCost = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_MANACOST));
            card.mPower = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_POWER));
            card.mToughness = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
            card.mLoyalty = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
            card.mText = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_ABILITY));
            card.mFlavor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
            card.mNumber = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
            card.setCode = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
            card.mCmc = cardCursor.getInt((cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC)));
            card.mColor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
            /* Override choice if the card can't be foil */
            if (!CardDbAdapter.canBeFoil(card.setCode, database)) {
                card.foil = false;
            }
            /* Clean up */
            cardCursor.close();

            /* Add it to the wishlist, either as a new CompressedWishlistInfo, or to an existing one */
            if (mCompressedDecklist.contains(card)) {
                boolean added = false;
                int firstIndex = mCompressedDecklist.indexOf(card);
                int lastIndex = mCompressedDecklist.lastIndexOf(card);
                if (firstIndex == lastIndex) {
                    CompressedDecklistInfo existingCard = mCompressedDecklist.get(firstIndex);
                    if (existingCard.mIsSideboard == isSideboard) {
                        for (IndividualSetInfo isi : existingCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            existingCard.add(card);
                        }
                    } else {
                        mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
                    }
                } else {
                    CompressedDecklistInfo firstCard = mCompressedDecklist.get(firstIndex);
                    CompressedDecklistInfo secondCard = mCompressedDecklist.get(lastIndex);
                    if (firstCard.mIsSideboard == isSideboard) {
                        for (IndividualSetInfo isi : firstCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            firstCard.add(card);
                        }
                    } else {
                        for (IndividualSetInfo isi : firstCard.mInfo) {
                            if (isi.mSetCode.equals(card.setCode) && isi.mIsFoil.equals(card.foil)) {
                                added = true;
                                isi.mNumberOf++;
                            }
                        }
                        if (!added) {
                            secondCard.add(card);
                        }
                    }
                }
            } else {
                mCompressedDecklist.add(new CompressedDecklistInfo(card, isSideboard));
            }

            /* The headers shouldn't (and can't) be sorted */
            clearHeaders();

            /* Sort the decklist */
            Collections.sort(mCompressedDecklist, mDecklistChain);

            /* Save the decklist */
            DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);

            /* Clean up for the next add */
            mNumberField.setText("1");
            mNameField.setText("");

            /* Redraw the new decklist with the new card */
            mDecklistAdapter.notifyDataSetChanged2();

        } catch (FamiliarDbException e) {
            handleFamiliarDbException(false);
        } catch (NumberFormatException e) {
            /* eat it */
        }
    }

    /**
     * read and compress the wishlist
     */
    @Override
    public void onResume() {
        super.onResume();
        mCompressedDecklist.clear();
        readAndCompressDecklist(null, mCurrentDeck);
        mDecklistAdapter.notifyDataSetChanged2();
    }

    @Override
    public void onPause() {
        super.onPause();
        DecklistHelpers.WriteCompressedDecklist(getActivity(), mCompressedDecklist);
    }

    /**
     * Read in the decklist from the file, and pack it into an ArrayList of CompressedDecklistInfo
     * for display in a ListView. This data structure stores one copy of the card itself, and a list
     * of set-specific attributes like the set name and rarity.
     * @param changedCardName
     */
    public void readAndCompressDecklist(String changedCardName, String deckName) {
        if (deckName == null || deckName.equals("") || deckName.equals(AUTOSAVE_NAME)) {
            deckName = AUTOSAVE_NAME;
            mDeckName.setText(R.string.decklist_unnamed_deck);
        } else {
            mDeckName.setText(deckName);
        }
        deckName += DECK_EXTENSION;
        /* Read the decklist */
        ArrayList<Pair<MtgCard, Boolean>> decklist = DecklistHelpers.ReadDecklist(getActivity(), deckName);
        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
        try {
            /* Translate the set code to TCG name of course it's not saved */
            for (Pair<MtgCard, Boolean> card : decklist) {
                card.first.setName = CardDbAdapter.getSetNameFromCode(card.first.setCode, database);
            }
            /* Clear the decklist, or just the card that changed */
            if (changedCardName == null) {
                mCompressedDecklist.clear();
            } else {
                for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                    if (cdi.mCard != null && cdi.mCard.mName.equals(changedCardName)) {
                        cdi.clearCompressedInfo();
                    }
                }
            }
            /* Compress the whole decklist, or just the card that changed */
            for (Pair<MtgCard, Boolean> card : decklist) {
                if (changedCardName == null || changedCardName.equals(card.first.mName)) {
                    if (!mCompressedDecklist.contains(card.first)) {
                        mCompressedDecklist.add(new CompressedDecklistInfo(card.first, card.second));
                    } else {
                        CompressedDecklistInfo existingCard = mCompressedDecklist.get(mCompressedDecklist.indexOf(card.first));
                        if (existingCard.mIsSideboard == card.second) {
                            mCompressedDecklist.get(mCompressedDecklist.indexOf(card.first)).add(card.first);
                        } else {
                            mCompressedDecklist.add(new CompressedDecklistInfo(card.first, card.second));
                        }
                    }
                }
            }
            /* check for wholly removed cards if one card was modified */
            if (changedCardName != null) {
                for (int i = 0; i < mCompressedDecklist.size(); i++) {
                    if (mCompressedDecklist.get(i).mInfo.size() == 0) {
                        mCompressedDecklist.remove(i);
                        i--;
                    }
                }
            }
            /* Fill extra card data from the database, for displaying full card info */
            CardDbAdapter.fillExtraWishlistData(mCompressedDecklist, database);
            setHeaderValues();
        } catch (FamiliarDbException fde) {
            handleFamiliarDbException(false);
        }
        DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
    }

    /**
     * This notifies the fragment when a change has been made from a card's dialog
     * @param cardName the card that was changed
     */
    @Override
    public void onWishlistChanged(String cardName) {
        readAndCompressDecklist(cardName, mCurrentDeck);
        clearHeaders();
        Collections.sort(mCompressedDecklist, mDecklistChain);
        mDecklistAdapter.notifyDataSetChanged2();
    }

    /**
     * Remove any showing dialogs, and show the requested one
     * @param id the ID of the dialog to show
     * @param cardName the name of the card to use if this is a dialog to change decklist counts
     * @param isSideboard if the card is in the sideboard
     * @throws IllegalStateException
     */
    private void showDialog(final int id, final String cardName, final boolean isSideboard) throws IllegalStateException {
        if (!this.isVisible()) {
            return;
        }
        removeDialog(getFragmentManager());
        DecklistDialogFragment newFragment = new DecklistDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        arguments.putString(DecklistDialogFragment.NAME_KEY, cardName);
        arguments.putBoolean(DecklistDialogFragment.SIDE_KEY, isSideboard);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Handle an ActionBar item click
     * @param item the item clicked
     * @return     true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* handle item selection */
        switch (item.getItemId()) {
            case R.id.deck_menu_save: {
                showDialog(DecklistDialogFragment.DIALOG_SAVE_DECK, null, false);
                return true;
            }
            case R.id.deck_menu_load: {
                showDialog(DecklistDialogFragment.DIALOG_LOAD_DECK, null, false);
                return true;
            }
            case R.id.deck_menu_delete: {
                showDialog(DecklistDialogFragment.DIALOG_DELETE_DECK, null, false);
                return true;
            }
            case R.id.deck_menu_clear: {
                showDialog(DecklistDialogFragment.DIALOG_CONFIRMATION, null, false);
                return true;
            }
            case R.id.deck_menu_share: {
                /* Share plaintext decklist */
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.decklist_share_title);
                sendIntent.putExtra(Intent.EXTRA_TEXT, DecklistHelpers.GetSharableDecklist(mCompressedDecklist, getActivity()));
                sendIntent.setType("text/plain");
                try {
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.decklist_share)));
                } catch (ActivityNotFoundException anfe) {
                    ToastWrapper.makeText(getActivity(), R.string.error_no_email_client, ToastWrapper.LENGTH_LONG).show();
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Removes all of the headers
     */
    private void clearHeaders() {
        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            if (mCompressedDecklist.get(i).mCard == null) { /* We found our header */
                /* Now remove it, and then back up a step */
                mCompressedDecklist.remove(i);
                i--;
            }
        }
    }

    /**
     * Inserts the headers for each type
     */
    private void setHeaderValues() {
        final String[] cardTypes = getResources().getStringArray(R.array.card_types_extra);
        final String[] cardHeaders = getResources().getStringArray(R.array.decklist_card_headers);
        ArrayList<String> insertedHeaders = new ArrayList<>();
        for (int i = 0; i < mCompressedDecklist.size(); i++) {
            for (int j = 0; j < cardTypes.length; j++) {
                CompressedDecklistInfo cdi = mCompressedDecklist.get(i);
                if (cdi.mCard != null && /* We only want entries that have a card attached */
                    (i == 0 || mCompressedDecklist.get(i - 1).header == null)) {
                    if (!cdi.mIsSideboard) {
                        if (j < cardHeaders.length - 1 && /* if j is in range */
                                cdi.mCard.mType.contains(cardTypes[j])) { /* the current card has the selected card type */
                            if (!insertedHeaders.contains(cardHeaders[j + 1])) {
                                mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false)); /* Add a new entry that will be our header */
                                mCompressedDecklist.get(i).header = cardHeaders[j + 1]; /* Use the header for the card type */
                                insertedHeaders.add(cardHeaders[j + 1]);
                            }
                            break;
                        } else if (j >= cardHeaders.length - 1) { /* j is out of bounds */
                            if (!insertedHeaders.contains(cardHeaders[cardHeaders.length - 1])) {
                                mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false)); /* Add a new entry that will be our header */
                                mCompressedDecklist.get(i).header = cardHeaders[cardHeaders.length - 1]; /* Use the last card header, "Other" */
                                insertedHeaders.add(cardHeaders[cardHeaders.length - 1]);
                            }
                            break;
                        }
                    } else if (!insertedHeaders.contains(cardHeaders[0])) { /* it is sideboard, if sideboard header hasn't already been added */
                        mCompressedDecklist.add(i, new CompressedDecklistInfo(null, false));
                        mCompressedDecklist.get(i).header = cardHeaders[0];
                        insertedHeaders.add(cardHeaders[0]);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.decklist_menu, menu);
    }

    public class CardDataAdapter extends RecyclerView.Adapter<CardDataAdapter.ViewHolder> {

        private static final int PENDING_REMOVAL_TIMEOUT = 3000; /* 3 seconds */

        private ArrayList<CompressedDecklistInfo> compressedCardInfos;
        private ArrayList<CompressedDecklistInfo> itemsPendingRemoval = new ArrayList<>();

        private Handler handler = new Handler();
        HashMap<CompressedDecklistInfo, Runnable> pendingRunnables = new HashMap<>();

        private View.OnClickListener mClickListener;

        public CardDataAdapter(ArrayList<CompressedDecklistInfo> values) {
            compressedCardInfos = values;
        }

        @Override
        public CardDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(CardDataAdapter.ViewHolder holder, int position) {
            if (position >= compressedCardInfos.size()) {
                return;
            }
            final DecklistHelpers.CompressedDecklistInfo info = compressedCardInfos.get(position);
            if (itemsPendingRemoval.contains(info)) { /* if the item IS pending removal */
                holder.itemView.setBackgroundColor(Color.RED);
                holder.itemView.findViewById(R.id.card_row).setVisibility(View.GONE);
                holder.itemView.findViewById(R.id.decklistSeparator).setVisibility(View.GONE);
                holder.undoButton.setVisibility(View.VISIBLE);
                holder.undoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Runnable pendingRemovalRunnable = pendingRunnables.get(info);
                        pendingRunnables.remove(info);
                        if (pendingRemovalRunnable != null) {
                            handler.removeCallbacks(pendingRemovalRunnable);
                        }
                        itemsPendingRemoval.remove(info);
                        notifyItemChanged(compressedCardInfos.indexOf(info));
                    }
                });
            } else { /* if the item IS NOT pending removal */
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                if (info.header != null) {
                    holder.itemView.setOnClickListener(null);
                    holder.itemView.findViewById(R.id.decklistSeparator).setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.GONE);
                    ((TextView) holder.itemView.findViewById(R.id.decklistHeaderType)).setText(info.header);
                    holder.swipeable = false;
                } else {
                    holder.itemView.findViewById(R.id.card_row).setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.decklistSeparator).setVisibility(View.GONE);
                    holder.undoButton.setVisibility(View.GONE);
                    View separator = holder.itemView.findViewById(R.id.decklistSeparator);
                    separator.setVisibility(View.GONE);
                    Html.ImageGetter imageGetter = ImageGetterHelper.GlyphGetter(getActivity());
                    holder.cardName.setText(info.mCard.mName);
                    holder.cardQuantity.setText(String.valueOf(info.getTotalNumber()));
                    holder.cardCost.setText(ImageGetterHelper.formatStringWithGlyphs(info.mCard.mManaCost, imageGetter));
                }
            }
        }

        /**
         * Get the number of cards of the type to display in the header
         * @param headerValue the card type we are counting
         * @return the number of cards of the given type
         */
        private int getTotalNumberOfType(final String headerValue) {
            int totalCards = 0;
            String currentHeader = "";
            for (CompressedDecklistInfo cdi : compressedCardInfos) {
                /* check if one of two things is correct
                 * 1. the card is a sideboard card
                 * 2. the card's header is the headerValue, and isn't in the sideboard */
                if (cdi.header != null) {
                    currentHeader = cdi.header;
                    continue;
                }
                if ((headerValue.equals(getString(R.string.decklist_sideboard)) && cdi.mIsSideboard)
                        || (currentHeader.equals(headerValue) && !cdi.mIsSideboard)) {
                    totalCards += cdi.getTotalNumber();
                }
            }
            return totalCards;
        }

        /**
         * Get the total number of cards in this adapter
         * @return the total number of cards
         */
        int getTotalCards() {
            int totalCards = 0;
            for (CompressedDecklistInfo cdi : compressedCardInfos) {
                totalCards += cdi.getTotalNumber();
            }
            return totalCards;
        }

        @Override
        public int getItemCount() {
            return compressedCardInfos.size();
        }

        void pendingRemoval(int position) {
            final CompressedDecklistInfo info = compressedCardInfos.get(position);
            if (!itemsPendingRemoval.contains(info)) {
                itemsPendingRemoval.add(info);
                notifyItemChanged(position);
                Runnable pendingRemovalRunnable = new Runnable() {
                    @Override
                    public void run() {
                        remove(compressedCardInfos.indexOf(info));
                    }
                };
                handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
                pendingRunnables.put(info, pendingRemovalRunnable);
            }

        }

        public void remove(int position) {
            CompressedDecklistInfo info = compressedCardInfos.get(position);
            if (itemsPendingRemoval.contains(info)) {
                itemsPendingRemoval.remove(info);
            }
            if (compressedCardInfos.contains(info)) {
                compressedCardInfos.remove(info);
                notifyItemChanged(position);
            }
            notifyDataSetChanged2();
        }

        private void notifyDataSetChanged2() {
            String totalCards = String.valueOf(getTotalCards()) + " ";
            mDeckCards.setText(totalCards);
            clearHeaders();
            setHeaderValues();
            notifyDataSetChanged();
        }

        public void setOnClickListener(View.OnClickListener listener) {
            mClickListener = listener;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView cardName;
            private TextView cardQuantity;
            private TextView cardCost;
            private TextView undoButton;
            public boolean swipeable = true;

            ViewHolder(ViewGroup view) {
                super(LayoutInflater.from(view.getContext()).inflate(R.layout.decklist_card_row, view, false));
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mClickListener.onClick(view);
                    }
                });
                cardName = (TextView) itemView.findViewById(R.id.decklistRowName);
                cardQuantity = (TextView) itemView.findViewById(R.id.decklistRowNumber);
                cardCost = (TextView) itemView.findViewById(R.id.decklistRowCost);
                undoButton = (TextView) itemView.findViewById(R.id.undo_button);
            }

        }

    }

}
