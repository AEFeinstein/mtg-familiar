package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.LcPlayerDialogFragment;

import java.util.ArrayList;

public class LcPlayer {
    /* Handler for committing life changes */
    private final Handler mHandler = new Handler();
    /* Reference to parent fragment */
    private final LifeCounterFragment mFragment;
    /* Histories and adapters for life, poison, commander damage */
    public ArrayList<HistoryEntry> mLifeHistory = new ArrayList<>();
    public ArrayList<HistoryEntry> mPoisonHistory = new ArrayList<>();
    public ArrayList<CommanderEntry> mCommanderDamage = new ArrayList<>();
    public CommanderDamageAdapter mCommanderDamageAdapter;

    /* Stats */
    public int mLife = LifeCounterFragment.DEFAULT_LIFE;
    public int mPoison = 0;
    public String mName;
    public int mCommanderCasting = 0;
    public int mDefaultLifeTotal = LifeCounterFragment.DEFAULT_LIFE;
    /* The player's View to be drawn in the LifeCounterFragment */
    public View mView;
    public View mCommanderRowView;
    private HistoryArrayAdapter mHistoryLifeAdapter;
    private HistoryArrayAdapter mHistoryPoisonAdapter;
    private boolean mCommitting = false;
    /* This runnable will commit any life / poison changes to history, and notify the adapter to display the changes
       in the ListView */
    private final Runnable mLifePoisonCommitter = new Runnable() {
        @Override
        public void run() {
            mCommitting = false;

            if (mLifeHistory.size() > 0 && mLifeHistory.get(0).mDelta == 0) {
                mLifeHistory.remove(0);
            }
            if (mPoisonHistory.size() > 0 && mPoisonHistory.get(0).mDelta == 0) {
                mPoisonHistory.remove(0);
            }

            if (mHistoryLifeAdapter != null) {
                mHistoryLifeAdapter.notifyDataSetChanged();
            }
            if (mHistoryPoisonAdapter != null) {
                mHistoryPoisonAdapter.notifyDataSetChanged();
            }
        }
    };
    public int mMode = LifeCounterFragment.STAT_LIFE;
    /* UI Elements */
    public TextView mNameTextView;
    public TextView mReadoutTextView;
    public TextView mCommanderNameTextView;
    private TextView mCommanderReadoutTextView;
    private Button mCommanderCastingButton;
    private ListView mHistoryList;

    /**
     * Constructor, save a reference to the parent fragment, and set a default name just in case
     *
     * @param fragment The parent fragment
     */
    public LcPlayer(LifeCounterFragment fragment) {
        mFragment = fragment;
        mName = mFragment.getActivity().getString(R.string.life_counter_default_name);
    }

    /**
     * Sets the display mode between life and poison. Switches the readout, commander readout, color, and history
     *
     * @param mode either STAT_LIFE, STAT_POISON, or STAT_COMMANDER
     */
    public void setMode(int mode) {

        /* Commit any changes before switching modes */
        if (mCommitting) {
            mCommitting = false;
            mHandler.removeCallbacks(mLifePoisonCommitter);
            mLifePoisonCommitter.run();
        }

        mMode = mode;
        switch (mMode) {
            case LifeCounterFragment.STAT_LIFE:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mHistoryLifeAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(mLife + "");
                mReadoutTextView.setTextColor(ContextCompat.getColor(mFragment.getContext(), 
                        R.color.material_red_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(mLife + "");
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_red_500));
                }
                break;
            case LifeCounterFragment.STAT_POISON:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mHistoryPoisonAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(mPoison + "");
                mReadoutTextView.setTextColor(ContextCompat.getColor(mFragment.getContext(), 
                        R.color.material_green_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(mPoison + "");
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_green_500));
                }
                break;
            case LifeCounterFragment.STAT_COMMANDER:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mCommanderDamageAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(mLife + "");
                mReadoutTextView.setTextColor(ContextCompat.getColor(mFragment.getContext(), 
                        R.color.material_red_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(mLife + "");
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_red_500));
                }
                break;
        }
    }

    /**
     * Convenience method to change the currently displayed value, either poison or life, and start the handler to
     * commit the value to history
     *
     * @param delta How much the current value should be changed
     */
    public void changeValue(int delta, boolean immediate) {
        switch (mMode) {
            case LifeCounterFragment.STAT_POISON:
                mPoison += delta;
                mReadoutTextView.setText(mPoison + "");
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(mPoison + "");
                }
                break;
            case LifeCounterFragment.STAT_COMMANDER:
            case LifeCounterFragment.STAT_LIFE:
                mLife += delta;
                mReadoutTextView.setText(mLife + "");
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(mLife + "");
                }
                break;
        }

        if (delta == 0) {
            return;
        }

        /* If we're not committing yet, make a new history entry */
        if (!mCommitting) {
            /* Create a new historyEntry */
            HistoryEntry entry = new HistoryEntry();
            /* If there are no entries, assume life is mDefaultLifeTotal */
            if (mLifeHistory.size() == 0) {
                entry.mDelta = mLife - mDefaultLifeTotal;
            } else {
                entry.mDelta = mLife - mLifeHistory.get(0).mAbsolute;
            }
            entry.mAbsolute = mLife;
            if (entry.mDelta != 0) {
                mLifeHistory.add(0, entry);
                if (mHistoryLifeAdapter != null) {
                    mHistoryLifeAdapter.notifyDataSetChanged();
                }
            }

            entry = new HistoryEntry();
            if (mPoisonHistory.size() == 0) {
                entry.mDelta = mPoison;
            } else {
                entry.mDelta = mPoison - mPoisonHistory.get(0).mAbsolute;
            }
            entry.mAbsolute = mPoison;
            if (entry.mDelta != 0) {
                mPoisonHistory.add(0, entry);
                if (mHistoryPoisonAdapter != null) {
                    mHistoryPoisonAdapter.notifyDataSetChanged();
                }
            }
        } else if (!immediate) {
            /* Modify current historyEntry */
            switch (mMode) {
                case LifeCounterFragment.STAT_POISON: {
                    mPoisonHistory.get(0).mDelta += delta;
                    mPoisonHistory.get(0).mAbsolute += delta;
                    if (null != mHistoryPoisonAdapter) {
                        mHistoryPoisonAdapter.notifyDataSetChanged();
                    }
                    break;
                }
                case LifeCounterFragment.STAT_COMMANDER:
                case LifeCounterFragment.STAT_LIFE: {
                    mLifeHistory.get(0).mDelta += delta;
                    mLifeHistory.get(0).mAbsolute += delta;
                    if (null != mHistoryLifeAdapter) {
                        mHistoryLifeAdapter.notifyDataSetChanged();
                    }
                    break;
                }
            }
        }

        if (!immediate) {
            mCommitting = true;
            mHandler.removeCallbacks(mLifePoisonCommitter);
            mHandler.postDelayed(mLifePoisonCommitter,
                    Integer.parseInt(mFragment.getFamiliarActivity().mPreferenceAdapter.getLifeTimer()));

        }
    }

    /**
     * Inflate the necessary views for this player, set the onClickListeners, and return the view to be added to the
     * GridView
     *
     * @param displayMode The display mode, either DISPLAY_COMMANDER, DISPLAY_COMPACT or DISPLAY_NORMAL
     * @param statType    The stat type being displayed, either STAT_POISON, STAT_LIFE, or STAT_COMMANDER
     * @return The view to be added to the GridView. Can either be mView or mCommanderRowView
     */
    public View newView(int displayMode, int statType) {
        switch (displayMode) {
            case LifeCounterFragment.DISPLAY_COMMANDER:
            case LifeCounterFragment.DISPLAY_NORMAL: {
                /* Inflate the player view */
                mView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player, null, false);
                assert mView != null;
                mHistoryList = (ListView) mView.findViewById(R.id.player_history);
                mCommanderCastingButton = (Button) mView.findViewById(R.id.commanderCast);

                /* Make new adapters */
                mHistoryLifeAdapter = new HistoryArrayAdapter(mFragment.getActivity(), LifeCounterFragment.STAT_LIFE);
                mHistoryPoisonAdapter
                        = new HistoryArrayAdapter(mFragment.getActivity(), LifeCounterFragment.STAT_POISON);
                mCommanderDamageAdapter = new CommanderDamageAdapter(mFragment.getActivity());

                /* If it's commander, also inflate the entry to display in the grid, and set up the casting button */
                if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                    mView.findViewById(R.id.commanderCastText).setVisibility(View.VISIBLE);
                    mCommanderCastingButton.setText("" + mCommanderCasting);
                    mCommanderCastingButton.setVisibility(View.VISIBLE);
                    mCommanderCastingButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mCommanderCasting++;
                            mCommanderCastingButton.setText("" + mCommanderCasting);
                        }
                    });
                    mCommanderCastingButton.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            mCommanderCasting--;
                            if (mCommanderCasting < 0) {
                                mCommanderCasting = 0;
                            }
                            mCommanderCastingButton.setText("" + mCommanderCasting);
                            return true;
                        }
                    });

                    mCommanderRowView = LayoutInflater.from(
                            mFragment.getActivity()).inflate(R.layout.life_counter_player_commander, null, false);
                    assert mCommanderRowView != null;
                    mCommanderNameTextView = (TextView) mCommanderRowView.findViewById(R.id.player_name);
                    if (mName != null) {
                        mCommanderNameTextView.setText(mName);
                    }
                    mCommanderReadoutTextView = (TextView) mCommanderRowView.findViewById(R.id.player_readout);
                }
                /* otherwise hide the commander casting button */
                else {
                    mView.findViewById(R.id.commanderCastText).setVisibility(View.GONE);
                    mCommanderCastingButton.setVisibility(View.GONE);
                }

                break;
            }
            case LifeCounterFragment.DISPLAY_COMPACT: {
                /* inflate the compact view */
                mView = LayoutInflater
                        .from(mFragment.getActivity()).inflate(R.layout.life_counter_player_compact, null, false);
                /* don't bother with adapters */
                mHistoryList = null;
                mHistoryLifeAdapter = null;
                mHistoryPoisonAdapter = null;
                mCommanderDamageAdapter = null;
                break;
            }
        }
        assert mView != null;

        /* Set the name, will be in either compact or normal mView */
        mNameTextView = (TextView) mView.findViewById(R.id.player_name);
        if (mName != null) {
            mNameTextView.setText(mName);
        }
        /* If the user touches the life total, pop a dialog to change it via keyboard */
        mReadoutTextView = (TextView) mView.findViewById(R.id.player_readout);
        mReadoutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(LcPlayerDialogFragment.DIALOG_CHANGE_LIFE, -1);
            }
        });

        /* Set the life / poison modifier buttons */
        mView.findViewById(R.id.player_minus1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeValue(-1, false);
            }
        });
        mView.findViewById(R.id.player_minus5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeValue(-5, false);
            }
        });
        mView.findViewById(R.id.player_plus1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeValue(1, false);
            }
        });
        mView.findViewById(R.id.player_plus5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeValue(5, false);
            }
        });

        mView.findViewById(R.id.player_name).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(LcPlayerDialogFragment.DIALOG_SET_NAME, -1);
            }
        });

        setMode(statType);

        if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
            return mCommanderRowView;
        } else {
            return mView;
        }
    }

    /**
     * Returns a string containing all the player data in the form:
     * name; life; life History; poison; poison History; default Life; commander History; commander casting
     * The history entries are comma delimited
     *
     * @return A string of player data
     */
    public String toString() {
        String data = mName.replace(";", "") + ";";

        boolean first = true;
        data += mLife + ";";
        for (HistoryEntry entry : mLifeHistory) {
            if (first) {
                first = false;
                data += entry.mAbsolute;
            } else {
                data += "," + entry.mAbsolute;
            }
        }

        data += ";";

        first = true;
        data += mPoison + ";";
        for (HistoryEntry entry : mPoisonHistory) {
            if (first) {
                first = false;
                data += entry.mAbsolute;
            } else {
                data += "," + entry.mAbsolute;
            }
        }

        data += ";" + mDefaultLifeTotal;

        first = true;
        for (CommanderEntry entry : mCommanderDamage) {
            if (first) {
                first = false;
                data += ";" + entry.mLife;
            } else {
                data += "," + entry.mLife;
            }
        }

        data += ";" + mCommanderCasting;

        return data + ";\n";
    }

    /**
     * Reset the life, poison, and commander damage to default while preserving the name and default life
     */
    public void resetStats() {
        mLifeHistory.clear();
        mPoisonHistory.clear();
        mLife = mDefaultLifeTotal;
        mPoison = 0;
        mCommanderCasting = 0;

        for (CommanderEntry entry : mCommanderDamage) {
            entry.mLife = 0;
        }

        if (mHistoryLifeAdapter != null) {
            mHistoryLifeAdapter.notifyDataSetChanged();
        }
        if (mHistoryPoisonAdapter != null) {
            mHistoryPoisonAdapter.notifyDataSetChanged();
        }
        if (mCommanderDamageAdapter != null) {
            mCommanderDamageAdapter.notifyDataSetChanged();
        }

        /* Check for -1 life? */
        if (mLife == -1) {
            mLife = LifeCounterFragment.DEFAULT_LIFE;
        }

        /* Redraw life totals */
        changeValue(0, true);
        if (mCommanderCastingButton != null) {
            mCommanderCastingButton.setText("" + mCommanderCasting);
        }
    }

    /**
     * Set the size of the player's view
     *
     * @param mGridLayoutWidth  The width of the GridLayout in which to put the player's view
     * @param mGridLayoutHeight The height of the GridLayout in which to put the player's view
     * @param mDisplayMode      either LifeCounterFragment.DISPLAY_COMPACT or LifeCounterFragment.DISPLAY_NORMAL
     */
    public void setSize(int mGridLayoutWidth, int mGridLayoutHeight, int mDisplayMode, boolean isPortrait) {

        switch (mDisplayMode) {
            case LifeCounterFragment.DISPLAY_NORMAL: {
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) mView.getLayoutParams();
                assert params != null;
                if (isPortrait) {
                    params.width = mGridLayoutWidth;
                    params.height = mGridLayoutHeight / 2;
                } else {
                    params.width = mGridLayoutWidth / 2;
                    params.height = mGridLayoutHeight;
                }
                mView.setLayoutParams(params);
                break;
            }
            case LifeCounterFragment.DISPLAY_COMPACT: {
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) mView.getLayoutParams();
                assert params != null;
                if (isPortrait) {
                    params.width = mGridLayoutWidth / 2;
                    params.height = mGridLayoutHeight / 2;
                } else {
                    params.width = mGridLayoutWidth / 4;
                    params.height = mGridLayoutHeight;
                }
                mView.setLayoutParams(params);
                break;
            }
            case LifeCounterFragment.DISPLAY_COMMANDER: {
                /* Set the row height to 48dp and the width to some fraction of the screen */
                GridLayout.LayoutParams rowParams = (GridLayout.LayoutParams) mCommanderRowView.getLayoutParams();
                assert rowParams != null;
                rowParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                        mFragment.getActivity().getResources().getDisplayMetrics());
                if (isPortrait) {
                    rowParams.width = mGridLayoutWidth / 2;
                } else {
                    rowParams.width = mGridLayoutWidth / 4;
                }
                mCommanderRowView.setLayoutParams(rowParams);

                /* Then set the player view to half the screen, if in landscape */
                LinearLayout.LayoutParams viewParams = (LinearLayout.LayoutParams) mView.getLayoutParams();
                if (viewParams != null) {
                    if (!isPortrait) {
                        viewParams.width = mGridLayoutWidth / 2;
                    }
                    mView.setLayoutParams(viewParams);
                }
                break;
            }
        }
    }

    /**
     * Called when the parent fragment pauses. Use this time to commit all pending transactions
     */
    public void onPause() {
        mHandler.removeCallbacks(mLifePoisonCommitter);
        mLifePoisonCommitter.run();
    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id       the ID of the dialog to show
     * @param position Which commander the player is taking damage from
     */
    private void showDialog(final int id, final int position) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        ((FamiliarActivity) mFragment.getActivity())
                .removeDialogFragment(mFragment.getActivity().getSupportFragmentManager());

        /* Create and show the dialog. */
        LcPlayerDialogFragment newFragment = new LcPlayerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        arguments.putInt(LcPlayerDialogFragment.POSITION_KEY, position);
        newFragment.setArguments(arguments);
        newFragment.setLcPlayer(this);
        newFragment.show(mFragment.getActivity().getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Inner class to encapsulate an entry in the history list
     */
    public static class HistoryEntry {
        public int mDelta;
        public int mAbsolute;
    }

    /**
     * Inner class to encapsulate an entry in the commander damage list
     */
    public static class CommanderEntry {
        public int mLife;
        public String mName;
    }

    /**
     * Inner class to display the HistoryEntries in a ListView
     */
    public class HistoryArrayAdapter extends ArrayAdapter<HistoryEntry> {

        private final int mType;

        /**
         * Constructor
         *
         * @param context a context to use in the superclass constructor
         * @param type    either STAT_LIFE or STAT_POISON
         */
        public HistoryArrayAdapter(Context context, int type) {
            super(context, R.layout.life_counter_history_adapter_row,
                    (type == LifeCounterFragment.STAT_LIFE) ? mLifeHistory : mPoisonHistory);
            mType = type;
        }

        /**
         * Called to get a view for an entry in the listView
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible.
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = LayoutInflater.from(mFragment.getActivity())
                        .inflate(R.layout.life_counter_history_adapter_row, null, false);
            }
            assert view != null;

            if (mCommitting && position == 0) {
                ((TextView) view.findViewById(R.id.absolute)).setTextColor(ContextCompat.getColor(mFragment.getContext(),
                        mFragment.getResourceIdFromAttr(R.attr.colorPrimary_attr)));
            } else {
                ((TextView) view.findViewById(R.id.absolute)).setTextColor(ContextCompat.getColor(mFragment.getContext(),
                        mFragment.getResourceIdFromAttr(R.attr.color_text)));
            }

            switch (mType) {
                case LifeCounterFragment.STAT_LIFE:
                    ((TextView) view.findViewById(R.id.absolute)).setText(mLifeHistory.get(position).mAbsolute + "");
                    if (mLifeHistory.get(position).mDelta > 0) {
                        ((TextView) view.findViewById(R.id.relative)).setText("+" + mLifeHistory.get(position).mDelta);
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(), 
                                        R.color.material_green_500)
                        );
                    } else {
                        ((TextView) view.findViewById(R.id.relative)).setText("" + mLifeHistory.get(position).mDelta);
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(), 
                                        R.color.material_red_500)
                        );
                    }
                    break;
                case LifeCounterFragment.STAT_POISON:
                    ((TextView) view.findViewById(R.id.absolute)).setText(mPoisonHistory.get(position).mAbsolute + "");
                    if (mPoisonHistory.get(position).mDelta > 0) {
                        ((TextView) view.findViewById(R.id.relative))
                                .setText("+" + mPoisonHistory.get(position).mDelta);
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(), 
                                        R.color.material_green_500)
                        );
                    } else {
                        ((TextView) view.findViewById(R.id.relative)).setText("" + mPoisonHistory.get(position).mDelta);
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(), 
                                        R.color.material_red_500)
                        );
                    }
                    break;
            }
            return view;
        }

    }

    /**
     * Inner class to display the HistoryEntries in a ListView
     */
    public class CommanderDamageAdapter extends ArrayAdapter<CommanderEntry> {

        /**
         * Constructor
         *
         * @param context a context to use in the superclass constructor
         */
        public CommanderDamageAdapter(Context context) {
            super(context, R.layout.life_counter_player_commander, mCommanderDamage);
        }

        /**
         * Called to get a view for an entry in the listView
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible.
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = LayoutInflater.from(mFragment.getActivity())
                        .inflate(R.layout.life_counter_player_commander, null, false);
            }
            assert view != null;

            ((TextView) view.findViewById(R.id.player_name)).setText(mCommanderDamage.get(position).mName + "");
            ((TextView) view.findViewById(R.id.player_readout)).setText(mCommanderDamage.get(position).mLife + "");

            view.findViewById(R.id.dividerH).setVisibility(View.GONE);
            view.findViewById(R.id.dividerV).setVisibility(View.GONE);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog(LcPlayerDialogFragment.DIALOG_COMMANDER_DAMAGE, position);
                }
            });
            return view;
        }

    }
}