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

package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.DeckStatsGenerator;
import com.gelakinetic.mtgfam.helpers.GraphHelper;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.view.ReliableColorPie;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DeckStatsFragment extends FamiliarFragment {
    private final List<MtgCard> mDeckToStat;
    /* UI Elements */
    private PieChart mTypeChart;
    private ReliableColorPie mColorChart;
    private BarChart mCmcChart;
    private GraphHelper mGraphHelper;

    /**
     * @param mDeckToStat The deck to generate stats from
     */
    public DeckStatsFragment(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
    }

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The view to be displayed.
     */
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.stat_frag, container, false);
        assert myFragmentView != null;
        DeckStatsGenerator mStatGenerator = new DeckStatsGenerator(mDeckToStat);
        mGraphHelper = new GraphHelper(mStatGenerator, requireContext());
        mTypeChart = myFragmentView.findViewById(R.id.type_chart);
        mColorChart = myFragmentView.findViewById(R.id.color_chart);
        mCmcChart = myFragmentView.findViewById(R.id.cmc_graph);
        mGraphHelper.fillStatGraphs(mTypeChart, mColorChart, mCmcChart);
        mTypeChart.invalidate(); //refresh
        mColorChart.invalidate(); //refresh
        mCmcChart.invalidate(); //refresh
        return myFragmentView;
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.stats_menu, menu);
    }

    /**
     * Handle an ActionBar item click.
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sample_hand) {
            startNewFragment(new SampleHandFrag(mDeckToStat), null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
