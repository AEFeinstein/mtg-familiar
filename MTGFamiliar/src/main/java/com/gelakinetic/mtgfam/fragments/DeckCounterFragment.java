package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.gelakinetic.mtgfam.R;

import java.util.ArrayList;

/**
 * This class helps judges tally deck counts quickly
 */
public class DeckCounterFragment extends FamiliarFragment implements ViewFactory {

    /* Keys for saving information on rotation */
    private static final String DECK_COUNT_KEY = "deck_count";
    private static final String SEQUENCE_KEY = "sequence";

    /* Static values to undo and clear the counts */
    private static final int COUNT_FLAG_UNDO = -1;
    private static final int COUNT_FLAG_RESET = -2;

    /* UI Elements */
    private TextSwitcher mDeckCountText;
    private TextView mDeckCountHistory;

    /* Deck counting data */
    private ArrayList<Integer> mDeckCountSequence;
    private int mDeckCount;

    /**
     * Set all the button actions, and restore any values from a previous state.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The view to be shown
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		/* Inflate the view, pull out UI elements */
        View myFragmentView = inflater.inflate(R.layout.deck_counter_frag, container, false);
        assert myFragmentView != null;
        mDeckCountText = (TextSwitcher) myFragmentView.findViewById(R.id.deck_counter_count);
        mDeckCountHistory = (TextView) myFragmentView.findViewById(R.id.deck_counter_history);

		/* Set the animations for the text switcher */
        mDeckCountText.setFactory(this);
        mDeckCountText.setInAnimation(AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_in_left));
        mDeckCountText.setOutAnimation(AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_out_right));

		/* Restore any state, if available */
        if (savedInstanceState != null) {
            mDeckCount = savedInstanceState.getInt(DECK_COUNT_KEY);
            mDeckCountSequence = StringToArray(savedInstanceState.getString(SEQUENCE_KEY));
        } else {
            mDeckCount = 0;
            mDeckCountSequence = new ArrayList<>();
        }

        StringBuilder history = new StringBuilder();
        for (Integer aSequence : mDeckCountSequence) {
            history.append(aSequence).append("  ");
        }
        mDeckCountHistory.setText(history.toString());
        mDeckCountText.setText("" + mDeckCount);

		/* Attach actions to all the buttons */
        myFragmentView.findViewById(R.id.deck_counter_1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(1);
            }
        });
        myFragmentView.findViewById(R.id.deck_counter_2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(2);
            }
        });
        myFragmentView.findViewById(R.id.deck_counter_3).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(3);
            }
        });
        myFragmentView.findViewById(R.id.deck_counter_4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(4);
            }
        });

        myFragmentView.findViewById(R.id.deck_counter_undo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(COUNT_FLAG_UNDO);
            }
        });

        myFragmentView.findViewById(R.id.deck_counter_reset).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardCount(COUNT_FLAG_RESET);
            }
        });
        return myFragmentView;
    }

    /**
     * Necessary to implement ViewFactory, this view will be used in the text switcher
     *
     * @return A view to be displayed in the text switcher
     */
    @Override
    public View makeView() {
        TextView t = new TextView(this.getActivity());
        t.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL);
        t.setTextAppearance(this.getActivity(), R.style.text_medium);
        t.setTextSize(70);
        return t;
    }

    /**
     * Save the deck count and history for rotation
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt(DECK_COUNT_KEY, mDeckCount);
        outState.putString(SEQUENCE_KEY, ArrayToString(mDeckCountSequence));

        super.onSaveInstanceState(outState);
    }

    /**
     * Used to persist an array list through a bundle as a string
     *
     * @param list The ArrayList to persist
     * @return A String representation of the list
     */
    private String ArrayToString(ArrayList<Integer> list) {
        String string = "";
        for (Integer i : list) {
            string += i + ",";
        }
        return string;
    }

    /**
     * Used to turn a persisted String into an ArrayList
     *
     * @param string The persisted String
     * @return An ArrayList built from the String
     */
    private ArrayList<Integer> StringToArray(String string) {
        String parts[] = string.split(",");
        ArrayList<Integer> list = new ArrayList<>(parts.length - 1);
        for (String part : parts) {
            try {
                list.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                /* ignore this entry */
            }
        }
        return list;
    }

    /**
     * Update the card count, and the sequence history
     *
     * @param count The number of cards to increment, COUNT_FLAG_UNDO, or COUNT_FLAG_RESET
     */
    private void updateCardCount(int count) {
        boolean updateUi = true;

        switch (count) {
            case COUNT_FLAG_UNDO:
                if (mDeckCountSequence.size() > 0) {
                    mDeckCount -= mDeckCountSequence.remove(mDeckCountSequence.size() - 1);
                } else {
                    updateUi = false;
                }
                break;
            case COUNT_FLAG_RESET:
                if (mDeckCountSequence.size() > 0) {
                    mDeckCount = 0;
                    mDeckCountSequence.clear();
                } else {
                    updateUi = false;
                }
                break;
            default:
                mDeckCount += count;
                mDeckCountSequence.add(count);
                break;
        }

        if (updateUi) {
            StringBuilder history = new StringBuilder();
            for (Integer aSequence : mDeckCountSequence) {
                history.append(aSequence).append("  ");
            }
            mDeckCountHistory.setText(history.toString());
            mDeckCountText.setText("" + mDeckCount);
        }
    }
}
